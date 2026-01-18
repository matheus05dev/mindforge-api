package com.matheusdev.mindforge.ai.service;

import com.matheusdev.mindforge.ai.chat.model.ChatMessage;
import com.matheusdev.mindforge.ai.chat.model.ChatSession;
import com.matheusdev.mindforge.ai.dto.ChatRequest;
import com.matheusdev.mindforge.ai.dto.PromptPair;
import com.matheusdev.mindforge.ai.memory.model.UserProfileAI;
import com.matheusdev.mindforge.ai.memory.service.MemoryService;
import com.matheusdev.mindforge.ai.provider.AIProvider;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.ai.service.model.InteractionType;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIOrchestrationService {

    private final Map<String, AIProvider> aiProviders;
    private final MemoryService memoryService;
    private final PromptBuilderService promptBuilderService;
    private final ChatService chatService;
    private final SmartRouterService smartRouterService;
    private final RAGService ragService;
    private final DocumentAnalyzer documentAnalyzer;

    private static final String DEFAULT_PROVIDER = "ollamaProvider";
    private static final String FALLBACK_PROVIDER = "groqProvider";
    private static final int OLLAMA_CHAR_LIMIT = 5000;

    private final Semaphore semaphore = new Semaphore(1);

    public CompletableFuture<AIProviderResponse> handleChatInteraction(ChatRequest chatRequest) {
        log.info(">>> [ORCHESTRATOR] Iniciando interação de chat.");

        String providerName = getProviderName(chatRequest.provider());
        AIProvider selectedProvider = getProvider(providerName);

        log.info("Provedor selecionado: {}", providerName);

        AIProviderRequest request = new AIProviderRequest(
                chatRequest.prompt(),
                chatRequest.systemMessage(),
                chatRequest.model(),
                providerName
        );

        return executeAndLogTask(request, selectedProvider, "chat interaction");
    }

    public CompletableFuture<AIProviderResponse> handleFileAnalysis(String userPrompt, String providerName, MultipartFile file) throws IOException {
        log.info(">>> [ORCHESTRATOR] Iniciando análise de arquivo: {}", file.getOriginalFilename());

        final Long userId = 1L;
        UserProfileAI userProfile = memoryService.getProfile(userId);
        log.info("Perfil do usuário carregado: {}", userProfile.getSummary());

        List<Document> langchainDocuments = new ArrayList<>();
        boolean isImage = file.getContentType() != null && file.getContentType().startsWith("image/");

        if (!isImage) {
            TikaDocumentReader documentReader = new TikaDocumentReader(file.getResource());
            List<org.springframework.ai.document.Document> springDocuments = documentReader.get();
            langchainDocuments = springDocuments.stream()
                    .map(springDoc -> Document.from(springDoc.getContent(), new Metadata(springDoc.getMetadata())))
                    .collect(Collectors.toList());
            log.info("Documentos extraídos e convertidos para o formato LangChain4j. Total: {}.", langchainDocuments.size());
        }

        String selectedProviderName = providerName;
        
        if (providerName == null || providerName.isBlank() || "null".equalsIgnoreCase(providerName)) {
            if (isImage) {
                selectedProviderName = "geminiProvider";
                log.info("🤖 Smart Routing: Imagem detectada -> Usando Gemini.");
            } else {
                long totalChars = langchainDocuments.stream().mapToLong(doc -> doc.text().length()).sum();
                if (totalChars > OLLAMA_CHAR_LIMIT) {
                    selectedProviderName = FALLBACK_PROVIDER;
                    log.info("🤖 Smart Routing: Texto GRANDE ({} chars > {}) -> Usando Groq (Cloud/Rápido).", totalChars, OLLAMA_CHAR_LIMIT);
                } else {
                    selectedProviderName = DEFAULT_PROVIDER;
                    log.info("🤖 Smart Routing: Texto PEQUENO ({} chars <= {}) -> Usando Ollama (Local/Privado).", totalChars, OLLAMA_CHAR_LIMIT);
                }
            }
        } else {
            selectedProviderName = getProviderName(providerName);
            log.info("🔒 Provedor forçado pelo usuário: {}", selectedProviderName);
        }

        AIProvider selectedProvider = getProvider(selectedProviderName);

        ChatSession session = chatService.createDocumentAnalysisSession(file.getOriginalFilename(), userPrompt);
        log.info("Sessão de chat criada: {}", session.getId());

        String userMessageContent = String.format("Arquivo: %s\n\nPrompt: %s", file.getOriginalFilename(), userPrompt);
        ChatMessage userMessage = chatService.saveMessage(session, "user", userMessageContent);
        log.info("Mensagem do usuário salva no banco: {}", userMessage.getId());

        PromptPair basePrompts = promptBuilderService.buildGenericPrompt(userPrompt, userProfile, Optional.empty(), Optional.empty());
        log.info("Prompt de sistema base gerado.");

        String finalSystemPrompt = basePrompts.systemPrompt();
        if ("groqProvider".equalsIgnoreCase(selectedProviderName)) {
            finalSystemPrompt += "\n\nVocê é um analista técnico. Se encontrar tabelas, extraia os valores numéricos com precisão de 100%. Nunca misture dados de exemplos práticos com definições teóricas do framework. Priorize e diferencie informações com base em sua fonte (metadados).";
        }

        if (isImage) {
            log.info("Iniciando fluxo de análise de imagem.");
            AIProviderRequest request = new AIProviderRequest(
                    userPrompt,
                    finalSystemPrompt,
                    null,
                    selectedProviderName,
                    true,
                    file.getBytes(),
                    file.getContentType(),
                    null,
                    null
            );
            return executeAndLogTask(request, selectedProvider, "análise de imagem")
                    .thenCompose(response -> saveResponseAndUpdateProfile(response, session, userMessage, userId, InteractionType.DOCUMENT_ANALYSIS));

        } else {
            String documentId = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document_" + System.currentTimeMillis();
            Document mainDocument = langchainDocuments.get(0);

            SmartRouterService.ProcessingStrategy strategy = smartRouterService.decideStrategy(mainDocument.text().length());

            return switch (strategy) {
                case ONE_SHOT -> processOneShot(mainDocument.text(), new PromptPair(finalSystemPrompt, basePrompts.userPrompt()), selectedProvider, selectedProviderName, userPrompt)
                        .thenCompose(response -> saveResponseAndUpdateProfile(response, session, userMessage, userId, InteractionType.DOCUMENT_ANALYSIS));
                
                case MAP_REDUCE -> {
                    log.info("Iniciando fluxo Map-Reduce para texto.");
                    boolean isLocalProvider = selectedProviderName.equalsIgnoreCase("ollamaProvider");

                    CompletableFuture<AIProviderResponse> result;
                    if (isLocalProvider) {
                        result = processChunksSequentially(langchainDocuments, new PromptPair(finalSystemPrompt, basePrompts.userPrompt()), selectedProvider, selectedProviderName, userPrompt);
                    } else {
                        result = processChunksWithRateLimit(langchainDocuments, new PromptPair(finalSystemPrompt, basePrompts.userPrompt()), selectedProvider, selectedProviderName, userPrompt);
                    }
                    yield result.thenCompose(response -> saveResponseAndUpdateProfile(response, session, userMessage, userId, InteractionType.DOCUMENT_ANALYSIS));
                }
                
                case RAG -> processWithRAG(documentId, mainDocument, userPrompt, new PromptPair(finalSystemPrompt, basePrompts.userPrompt()), selectedProvider, selectedProviderName)
                        .thenCompose(response -> saveResponseAndUpdateProfile(response, session, userMessage, userId, InteractionType.RAG_ANALYSIS));
            };
        }
    }

    private CompletableFuture<AIProviderResponse> processChunksSequentially(
            List<Document> chunks,
            PromptPair basePrompts,
            AIProvider provider,
            String providerName,
            String userPrompt) {

        log.info("🔄 Processamento SEQUENCIAL iniciado (Otimizado para Ollama)");

        return CompletableFuture.supplyAsync(() -> {
            List<String> partialResults = new ArrayList<>();

            for (int i = 0; i < chunks.size(); i++) {
                Document chunk = chunks.get(i);
                log.info("📄 Processando chunk {}/{} sequencialmente...", i + 1, chunks.size());

                String mapPrompt = String.format(
                        "Analise e resuma esta parte do documento focado em extrair insights, pontos chave e conclusões parciais: \n\n---\n%s\n\n---",
                        chunk.text()
                );

                AIProviderRequest mapRequest = new AIProviderRequest(mapPrompt, basePrompts.systemPrompt(), null, providerName);

                try {
                    AIProviderResponse response = provider.executeTask(mapRequest).get();
                    partialResults.add(response.getContent());
                    log.info("✅ Chunk {}/{} processado com sucesso.", i + 1, chunks.size());
                } catch (InterruptedException | ExecutionException e) {
                    log.error("❌ Erro ao processar chunk {}/{}: {}", i + 1, chunks.size(), e.getMessage());
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            return reduceResults(partialResults, basePrompts, provider, providerName, userPrompt);
        });
    }

    private CompletableFuture<AIProviderResponse> processChunksWithRateLimit(
            List<Document> chunks,
            PromptPair basePrompts,
            AIProvider provider,
            String providerName,
            String userPrompt) {

        log.info("🚦 Iniciado 'Throughput Shaping' para Groq. Processamento sequencial com cooldown.");

        return CompletableFuture.supplyAsync(() -> {
            List<String> partialResults = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                Document chunk = chunks.get(i);
                try {
                    semaphore.acquire();
                    log.info("📄 Processando chunk {}/{} ({} tokens)...", i + 1, chunks.size(), 800);

                    String mapPrompt = String.format(
                            "Analise e resuma esta parte do documento focado em extrair insights, pontos chave e conclusões parciais: \n\n---\n%s\n\n---",
                            chunk.text()
                    );
                    AIProviderRequest mapRequest = new AIProviderRequest(mapPrompt, basePrompts.systemPrompt(), null, providerName);

                    AIProviderResponse response = provider.executeTask(mapRequest).get();
                    partialResults.add(response.getContent());
                    log.info("✅ Chunk {}/{} processado. Aguardando cooldown...", i + 1, chunks.size());

                    if (i < chunks.size() - 1) {
                        log.info("⏳ Cooldown de 25 segundos para respeitar o limite de TPM do Groq.");
                        Thread.sleep(25000);
                    }

                } catch (InterruptedException | ExecutionException e) {
                    log.error("❌ Erro ao processar chunk {}/{}: {}", i + 1, chunks.size(), e.getMessage());
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    break; 
                } finally {
                    semaphore.release();
                }
            }
            log.info("✅ Todos os chunks processados. Iniciando redução final.");
            return reduceResults(partialResults, basePrompts, provider, providerName, userPrompt);
        });
    }

    private AIProviderResponse reduceResults(List<String> partialResults, PromptPair basePrompts, AIProvider provider, String providerName, String userPrompt) {
        if (partialResults.isEmpty()) {
            log.warn("Nenhum resultado parcial foi gerado. A redução foi abortada.");
            return new AIProviderResponse("Não foi possível gerar um resumo pois a análise inicial falhou.", "Análise inicial falhou.");
        }
        String combinedPartials = String.join("\n\n---\n\n", partialResults);
        log.info("Tamanho do texto combinado para redução: {} caracteres.", combinedPartials.length());

        String reducePrompt = String.format(
                "Junte as análises parciais a seguir em um relatório final coeso e bem estruturado, respondendo à solicitação original do usuário. Solicitação do usuário: '%s'.\n\n--- ANÁLISES PARCIAIS ---\n%s",
                userPrompt, combinedPartials
        );

        AIProviderRequest reduceRequest = new AIProviderRequest(reducePrompt, basePrompts.systemPrompt(), null, providerName);
        
        try {
            return executeAndLogTask(reduceRequest, provider, "análise final (Reduce)").get();
        } catch (InterruptedException | ExecutionException e) {
             throw new RuntimeException("Erro na redução final", e);
        }
    }

    private CompletableFuture<AIProviderResponse> processOneShot(
            String documentContent,
            PromptPair basePrompts,
            AIProvider provider,
            String providerName,
            String userPrompt) {

        log.info("🚀 Processamento ONE-SHOT: Enviando documento completo diretamente para a IA.");

        String fullPrompt = String.format(
                "%s\n\n--- CONTEÚDO DO DOCUMENTO ---\n%s\n\n--- FIM DO DOCUMENTO ---\n\nCom base no conteúdo acima, %s",
                userPrompt, documentContent, userPrompt
        );

        AIProviderRequest request = new AIProviderRequest(
                fullPrompt,
                basePrompts.systemPrompt(),
                null,
                providerName
        );

        return executeAndLogTask(request, provider, "análise one-shot");
    }

    private CompletableFuture<AIProviderResponse> processWithRAG(
            String documentId,
            Document document,
            String userPrompt,
            PromptPair basePrompts,
            AIProvider provider,
            String providerName) {

        log.info("🔍 Processamento RAG: Buscando segmentos relevantes usando busca semântica.");

        return CompletableFuture.supplyAsync(() -> {
            try {
                DocumentAnalyzer.DocumentProfile profile = documentAnalyzer.analyzeDocument(document.text());

                String systemPrompt = basePrompts.systemPrompt();
                if (profile.numericInferenceRisk) {
                    log.warn("⚠️ Risco de inferência numérica detectado! Injetando prompt de segurança.");
                    systemPrompt += "\n\n" +
                            "⚠️ Documento contém expressões matemáticas e fórmulas.\n" +
                            "É estritamente proibido:\n" +
                            "- realizar cálculos\n" +
                            "- interpretar intervalos\n" +
                            "- inferir porcentagens\n" +
                            "- deduzir métricas não explicitamente escritas\n" +
                            "Somente valores numéricos LITERALMENTE presentes no texto natural podem ser citados.";
                }

                List<TextSegment> relevantSegments = ragService.processQueryWithRAG(
                        documentId, document, userPrompt, 8
                );

                if (relevantSegments.isEmpty()) {
                    log.warn("Nenhum segmento relevante encontrado. Retornando análise genérica.");
                    String fallbackPrompt = String.format(
                            "O documento '%s' foi analisado, mas nenhum segmento específico foi encontrado para a pergunta: '%s'. " +
                            "Forneça uma análise geral do documento baseada no contexto disponível.",
                            documentId, userPrompt
                    );
                    AIProviderRequest fallbackRequest = new AIProviderRequest(
                            fallbackPrompt,
                            systemPrompt,
                            null,
                            providerName
                    );
                    return executeAndLogTask(fallbackRequest, provider, "análise RAG (fallback)").get();
                }

                String segmentsText = relevantSegments.stream()
                        .map(segment -> {
                            Map<String, Object> metadata = segment.metadata().toMap();
                            String metadataStr = String.format(
                                "[S:%s|T:%s%s]",
                                metadata.getOrDefault("section", "-"),
                                metadata.getOrDefault("content_type", "txt"),
                                metadata.containsKey("has_table") ? "|tbl" : ""
                            );
                            return String.format(
                                "\n-- Trecho %s --\n%s\n",
                                metadataStr,
                                segment.text()
                            );
                        })
                        .collect(Collectors.joining());

                String ragPrompt = String.format(
                    """
                    **Instrução:** Você é um analista de documentos. Responda à pergunta do usuário usando APENAS os trechos fornecidos.
                    
                    **Regras:**
                    1.  **Fonte:** Use apenas os trechos abaixo. Se a resposta não estiver neles, diga "A informação não foi encontrada".
                    2.  **Precisão:** Cite números e dados de tabelas ([tbl]) exatamente como estão.
                    3.  **Contexto:** Use os metadados ([S:seção|T:tipo]) para dar contexto.
                    4.  **Não invente:** Nunca adivinhe ou infira informações.
                    
                    **Pergunta:** %s
                    
                    **Trechos Relevantes:**
                    %s
                    
                    **Resposta:**
                    """,
                    userPrompt, segmentsText
                );

                log.info("📤 Enviando prompt RAG com {} segmentos para '{}'.", relevantSegments.size(), providerName);

                AIProviderRequest ragRequest = new AIProviderRequest(
                        ragPrompt,
                        systemPrompt,
                        null,
                        providerName
                );

                return executeAndLogTask(ragRequest, provider, "análise RAG").get();

            } catch (InterruptedException | ExecutionException e) {
                log.error("Erro ao processar documento com RAG: {}", e.getMessage(), e);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new RuntimeException("Erro no processamento RAG", e);
            }
        });
    }

    private CompletableFuture<AIProviderResponse> saveResponseAndUpdateProfile(
            AIProviderResponse response,
            ChatSession session,
            ChatMessage userMessage,
            Long userId,
            InteractionType type) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String contentToSave = response.getContent();
                if (contentToSave == null || contentToSave.isBlank()) {
                    contentToSave = "Erro: Resposta vazia do provedor de IA.";
                }

                ChatMessage assistantMessage = chatService.saveMessage(session, "assistant", contentToSave);
                log.info("Resposta do assistente salva no banco: {}", assistantMessage.getId());

                if (type == InteractionType.CHAT) {
                    List<Map<String, String>> chatHistory = List.of(
                        Map.of("role", "user", "content", userMessage.getContent()),
                        Map.of("role", "assistant", "content", contentToSave)
                    );

                    memoryService.updateUserProfile(userId, chatHistory);
                    log.info("Perfil do usuário atualizado (CHAT).");
                } else {
                    log.info("Memória ignorada para interação do tipo {}", type);
                }

                return response;
            } catch (Exception e) {
                log.error("Erro ao salvar resposta no banco ou atualizar perfil: {}", e.getMessage(), e);
                return response;
            }
        });
    }

    private CompletableFuture<AIProviderResponse> executeAndLogTask(AIProviderRequest request, AIProvider provider, String taskName) {
        log.debug("Enviando requisição '{}' para o provedor '{}'", taskName, provider.getClass().getSimpleName());

        return provider.executeTask(request)
                .whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        log.error("!!! ERRO na execução da tarefa '{}': {}", taskName, throwable.getMessage(), throwable);
                    } else {
                        log.info("<<< SUCESSO na tarefa '{}'. Resposta recebida.", taskName);
                    }
                });
    }

    private String getProviderName(String provider) {
        return (provider == null || provider.isBlank()) ? DEFAULT_PROVIDER : provider;
    }

    private AIProvider getProvider(String providerName) {
        AIProvider provider = aiProviders.get(providerName);
        if (provider == null) {
            throw new IllegalArgumentException("Provedor de IA desconhecido: " + providerName);
        }
        return provider;
    }
}
