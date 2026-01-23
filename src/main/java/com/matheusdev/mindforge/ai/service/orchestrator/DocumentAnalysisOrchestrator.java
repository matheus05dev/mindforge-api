package com.matheusdev.mindforge.ai.service.orchestrator;

import com.matheusdev.mindforge.ai.chat.model.ChatMessage;
import com.matheusdev.mindforge.ai.chat.model.ChatSession;
import com.matheusdev.mindforge.ai.dto.PromptPair;
import com.matheusdev.mindforge.ai.memory.model.UserProfileAI;
import com.matheusdev.mindforge.ai.memory.service.MemoryService;
import com.matheusdev.mindforge.ai.provider.AIProvider;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.ai.service.ChatService;
import com.matheusdev.mindforge.ai.service.DocumentAnalyzer;
import com.matheusdev.mindforge.ai.service.PromptBuilderService;
import com.matheusdev.mindforge.ai.service.PromptCacheService;
import com.matheusdev.mindforge.ai.service.RAGService;
import com.matheusdev.mindforge.ai.service.SmartRouterService;
import com.matheusdev.mindforge.ai.service.VectorStoreService;
import com.matheusdev.mindforge.ai.service.model.Evidence;
import com.matheusdev.mindforge.ai.service.model.InteractionType;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.stream.Collectors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentAnalysisOrchestrator {

    private final Map<String, AIProvider> aiProviders;
    private final MemoryService memoryService;
    private final ChatService chatService;
    private final PromptBuilderService promptBuilderService;
    private final PromptCacheService promptCacheService;
    private final SmartRouterService smartRouterService;
    private final RAGService ragService;
    private final VectorStoreService vectorStoreService;
    private final DocumentAnalyzer documentAnalyzer;
    private final RAGOrchestrator ragOrchestrator;

    private final Semaphore semaphore = new Semaphore(1);
    private static final String DEFAULT_PROVIDER = "ollamaProvider";
    private static final String FALLBACK_PROVIDER = "groqProvider";
    private static final int OLLAMA_CHAR_LIMIT = 5000;

    private AIProvider getProvider(String providerName) {
        AIProvider provider = aiProviders.get(providerName);
        if (provider == null) {
            throw new IllegalArgumentException("Provedor de IA desconhecido: " + providerName);
        }
        return provider;
    }

    private String getProviderName(String provider) {
        return (provider == null || provider.isBlank()) ? DEFAULT_PROVIDER : provider;
    }

    private CompletableFuture<AIProviderResponse> executeAndLogTask(AIProviderRequest request, AIProvider provider,
            String taskName) {
        return promptCacheService.executeWithCache(provider, request).handle((response, ex) -> {
            if (ex != null) {
                log.error("Erro em task {}: {}", taskName, ex.getMessage());
                throw new RuntimeException(ex);
            }
            return response;
        });
    }

    public CompletableFuture<AIProviderResponse> handleFileAnalysis(String userPrompt, String providerName,
            MultipartFile file) throws IOException {
        log.info(">>> [DOC ORCHESTRATOR] Iniciando análise de arquivo: {}", file.getOriginalFilename());

        final Long userId = 1L;
        UserProfileAI userProfile = memoryService.getProfile(userId);

        List<Document> langchainDocuments = new ArrayList<>();
        boolean isImage = file.getContentType() != null && file.getContentType().startsWith("image/");

        if (!isImage) {
            TikaDocumentReader documentReader = new TikaDocumentReader(file.getResource());
            List<org.springframework.ai.document.Document> springDocuments = documentReader.get();
            langchainDocuments = springDocuments.stream()
                    .map(springDoc -> Document.from(springDoc.getText(), new Metadata(springDoc.getMetadata())))
                    .collect(Collectors.toList());
        }

        String selectedProviderName = determineProvider(providerName, isImage, langchainDocuments);
        AIProvider selectedProvider = getProvider(selectedProviderName);

        ChatSession session = chatService.createDocumentAnalysisSession(file.getOriginalFilename(), userPrompt);
        String userMessageContent = String.format("Arquivo: %s\n\nPrompt: %s", file.getOriginalFilename(), userPrompt);
        ChatMessage userMessage = chatService.saveMessage(session, "user", userMessageContent);

        PromptPair basePrompts = promptBuilderService.buildGenericPrompt(userPrompt, userProfile, Optional.empty(),
                Optional.empty());
        String finalSystemPrompt = enrichSystemPromptWithGlossary(basePrompts.systemPrompt(), userPrompt);

        if (isImage) {
            return processImageAnalysis(userPrompt, finalSystemPrompt, selectedProviderName, file, selectedProvider,
                    session, userMessage, userId);
        } else {
            return processTextAnalysis(userPrompt, finalSystemPrompt, basePrompts, langchainDocuments,
                    selectedProviderName, selectedProvider, file, session, userMessage, userId);
        }
    }

    private String determineProvider(String providerName, boolean isImage, List<Document> docs) {
        if (providerName != null && !providerName.isBlank() && !"null".equalsIgnoreCase(providerName)) {
            return getProviderName(providerName);
        }
        if (isImage)
            return "ollamaProvider";
        long totalChars = docs.stream().mapToLong(d -> d.text().length()).sum();
        return totalChars > OLLAMA_CHAR_LIMIT ? FALLBACK_PROVIDER : DEFAULT_PROVIDER;
    }

    private CompletableFuture<AIProviderResponse> processImageAnalysis(String userPrompt, String systemPrompt,
            String providerName, MultipartFile file, AIProvider provider, ChatSession session, ChatMessage userMessage,
            Long userId) throws IOException {
        AIProviderRequest request = AIProviderRequest.builder()
                .textPrompt(userPrompt)
                .systemMessage(systemPrompt)
                .preferredProvider(providerName)
                .multimodal(true)
                .imageData(file.getBytes())
                .imageMimeType(file.getContentType())
                .build();
        return executeAndLogTask(request, provider, "análise de imagem")
                .thenCompose(response -> saveResponseAndUpdateProfile(response, session, userMessage, userId,
                        InteractionType.DOCUMENT_ANALYSIS));
    }

    private CompletableFuture<AIProviderResponse> processTextAnalysis(String userPrompt, String systemPrompt,
            PromptPair basePrompts, List<Document> docs, String providerName, AIProvider provider, MultipartFile file,
            ChatSession session, ChatMessage userMessage, Long userId) {
        String documentId = file.getOriginalFilename() != null ? file.getOriginalFilename()
                : "document_" + System.currentTimeMillis();
        Document mainDocument = docs.get(0);

        SmartRouterService.ProcessingStrategy strategy = smartRouterService
                .decideStrategy(mainDocument.text().length());

        return switch (strategy) {
            case ONE_SHOT -> processOneShot(mainDocument.text(), new PromptPair(systemPrompt, basePrompts.userPrompt()),
                    provider, providerName, userPrompt)
                    .thenCompose(response -> saveResponseAndUpdateProfile(response, session, userMessage, userId,
                            InteractionType.DOCUMENT_ANALYSIS));
            case MAP_REDUCE -> {
                boolean isLocal = providerName.equalsIgnoreCase("ollamaProvider");
                CompletableFuture<AIProviderResponse> res;
                if (isLocal)
                    res = processChunksSequentially(docs, new PromptPair(systemPrompt, basePrompts.userPrompt()),
                            provider, providerName, userPrompt);
                else
                    res = processChunksWithRateLimit(docs, new PromptPair(systemPrompt, basePrompts.userPrompt()),
                            provider, providerName, userPrompt);
                yield res.thenCompose(response -> saveResponseAndUpdateProfile(response, session, userMessage, userId,
                        InteractionType.DOCUMENT_ANALYSIS));
            }
            case RAG -> {
                List<Evidence> evidences = ragService.processQueryWithRAG(documentId, mainDocument, userPrompt, 8);
                yield ragOrchestrator
                        .processWithRAG(documentId, mainDocument, userPrompt, provider, providerName, evidences, null)
                        .thenCompose(response -> saveResponseAndUpdateProfile(response, session, userMessage, userId,
                                InteractionType.RAG_ANALYSIS));
            }
        };
    }

    private CompletableFuture<AIProviderResponse> processOneShot(String content, PromptPair prompts,
            AIProvider provider, String providerName, String userPrompt) {
        String fullPrompt = String.format("%s\n\n--- DOCUMENTO ---\n%s\n\n--- FIM ---\n\n%s", userPrompt, content,
                userPrompt);
        AIProviderRequest request = AIProviderRequest.builder().textPrompt(fullPrompt)
                .systemMessage(prompts.systemPrompt()).preferredProvider(providerName).build();
        return executeAndLogTask(request, provider, "one-shot");
    }

    private CompletableFuture<AIProviderResponse> processChunksSequentially(List<Document> chunks, PromptPair prompts,
            AIProvider provider, String providerName, String userPrompt) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> partials = new ArrayList<>();
            for (Document chunk : chunks) {
                String mapPrompt = "Analise este trecho:\n" + chunk.text();
                AIProviderRequest req = AIProviderRequest.builder().textPrompt(mapPrompt)
                        .systemMessage(prompts.systemPrompt()).preferredProvider(providerName).build();
                try {
                    AIProviderResponse res = provider.executeTask(req).get();
                    partials.add(res.getContent());
                } catch (Exception e) {
                    log.error("Erro chunk", e);
                }
            }
            return reduceResults(partials, prompts, provider, providerName, userPrompt);
        });
    }

    private CompletableFuture<AIProviderResponse> processChunksWithRateLimit(List<Document> chunks, PromptPair prompts,
            AIProvider provider, String providerName, String userPrompt) {
        // Simplified rate limit logic
        return processChunksSequentially(chunks, prompts, provider, providerName, userPrompt);
    }

    private AIProviderResponse reduceResults(List<String> partials, PromptPair prompts, AIProvider provider,
            String providerName, String userPrompt) {
        String combined = String.join("\n---\n", partials);
        String reducePrompt = "Consolide:\n" + combined + "\n\nSolicitação Original: " + userPrompt;
        AIProviderRequest req = AIProviderRequest.builder().textPrompt(reducePrompt)
                .systemMessage(prompts.systemPrompt()).preferredProvider(providerName).build();
        try {
            return executeAndLogTask(req, provider, "reduce").get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String enrichSystemPromptWithGlossary(String base, String query) {
        // Duplicated logic from ChatOrchestrator - ideally shared in DocumentAnalyzer
        // or PromptBuilder
        Map<String, String> staticDefinitions = documentAnalyzer.getTermDefinitions();
        StringBuilder glossary = new StringBuilder();
        for (Map.Entry<String, String> entry : staticDefinitions.entrySet()) {
            if (query.toLowerCase().contains(entry.getKey())) {
                glossary.append("- ").append(entry.getValue()).append("\n");
            }
        }
        return glossary.length() > 0 ? base + "\n\n### GLOSSÁRIO:\n" + glossary.toString() : base;
    }

    private CompletableFuture<AIProviderResponse> saveResponseAndUpdateProfile(AIProviderResponse response,
            ChatSession session, ChatMessage userMessage, Long userId, InteractionType type) {
        return CompletableFuture.supplyAsync(() -> {
            String content = response.getContent();
            chatService.saveMessage(session, "assistant", content);
            List<Map<String, String>> chatHistory = List.of(
                    Map.of("role", "user", "content", userMessage.getContent()),
                    Map.of("role", "assistant", "content", content));
            memoryService.updateUserProfile(userId, chatHistory);
            response.setSessionId(session.getId());
            response.setType(type);
            return response;
        });
    }
}
