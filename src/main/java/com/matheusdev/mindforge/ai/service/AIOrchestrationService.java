package com.matheusdev.mindforge.ai.service;

import com.matheusdev.mindforge.ai.dto.ChatRequest;
import com.matheusdev.mindforge.ai.dto.PromptPair;
import com.matheusdev.mindforge.ai.memory.model.UserProfileAI;
import com.matheusdev.mindforge.ai.memory.service.MemoryService;
import com.matheusdev.mindforge.ai.provider.AIProvider;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIOrchestrationService {

    private final Map<String, AIProvider> aiProviders;
    private final MemoryService memoryService;
    private final PromptBuilderService promptBuilderService;
    private static final String DEFAULT_PROVIDER = "geminiProvider";
    private final Semaphore semaphore = new Semaphore(3); // Limita a 3 chamadas simultâneas

    public CompletableFuture<AIProviderResponse> handleChatInteraction(ChatRequest chatRequest) {
        log.info(">>> AIOrchestrationService: Iniciando interação de chat.");
        String providerName = getProviderName(chatRequest.provider());
        AIProvider selectedProvider = getProvider(providerName);

        AIProviderRequest request = new AIProviderRequest(
                chatRequest.prompt(),
                chatRequest.systemMessage(),
                chatRequest.model(),
                providerName
        );

        return executeAndLogTask(request, selectedProvider, "chat interaction");
    }

    public CompletableFuture<AIProviderResponse> handleFileAnalysis(String userPrompt, String providerName, MultipartFile file) throws IOException {
        log.info(">>> AIOrchestrationService: Iniciando análise de arquivo.");
        final Long userId = 1L; // Provisório
        UserProfileAI userProfile = memoryService.getProfile(userId);
        log.info("Perfil do usuário carregado: {}", userProfile.getSummary());

        String selectedProviderName = getProviderName(providerName);
        AIProvider selectedProvider = getProvider(selectedProviderName);

        PromptPair basePrompts = promptBuilderService.buildGenericPrompt(userPrompt, userProfile, Optional.empty(), Optional.empty());
        log.info("Prompt de sistema gerado: '{}'", basePrompts.systemPrompt());

        if (file.getContentType() != null && file.getContentType().startsWith("image/")) {
            log.info("Arquivo detectado como imagem. Preparando requisição multimodal.");
            AIProviderRequest request = new AIProviderRequest(userPrompt, basePrompts.systemPrompt(), null, selectedProviderName, true, file.getBytes(), file.getContentType(), null, null);
            return executeAndLogTask(request, selectedProvider, "análise de arquivo multimodal");
        } else {
            log.info("Arquivo detectado como documento de texto. Iniciando fluxo Map-Reduce.");

            TikaDocumentReader documentReader = new TikaDocumentReader(file.getResource());
            List<Document> documents = documentReader.get();
            log.info("Documento lido com Tika. Tamanho total: {} bytes.", file.getSize());

            // 1. Chunking (Splitting)
            TokenTextSplitter textSplitter = new TokenTextSplitter(4000, 200, 5, 10000, true);
            List<Document> chunkedDocuments = textSplitter.apply(documents);
            log.info("Documento dividido em {} partes (chunks).", chunkedDocuments.size());

            // 2. Map Step com controle de concorrência via Semaphore
            List<CompletableFuture<AIProviderResponse>> mapFutures = chunkedDocuments.stream()
                    .map(chunk -> CompletableFuture.supplyAsync(() -> {
                        try {
                            log.debug("Aguardando permissão do semáforo...");
                            semaphore.acquire(); // Pega uma "licença"
                            log.debug("Permissão concedida. Processando chunk...");
                            
                            String mapPrompt = String.format("Analise e resuma esta parte do documento focado em extrair insights, pontos chave e conclusões parciais: \n\n---\n%s\n\n---", chunk.getContent());
                            AIProviderRequest mapRequest = new AIProviderRequest(mapPrompt, basePrompts.systemPrompt(), null, selectedProviderName);
                            
                            // .join() é crucial aqui para garantir que a tarefa espere a conclusão da chamada de rede
                            return selectedProvider.executeTask(mapRequest).join();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            log.error("Thread interrompida durante a aquisição do semáforo.", e);
                            throw new RuntimeException(e);
                        } finally {
                            semaphore.release(); // Libera para o próximo chunk
                            log.debug("Permissão do semáforo liberada.");
                        }
                    }))
                    .collect(Collectors.toList());

            CompletableFuture<Void> allOfMap = CompletableFuture.allOf(mapFutures.toArray(new CompletableFuture[0]));

            // 3. Reduce Step
            return allOfMap.thenCompose(v -> {
                String combinedPartials = mapFutures.stream()
                        .map(CompletableFuture::join)
                        .map(AIProviderResponse::getContent)
                        .collect(Collectors.joining("\n\n---\n\n"));
                log.info("Resultados parciais combinados. Tamanho total: {} caracteres.", combinedPartials.length());

                String reducePrompt = String.format(
                        "Junte as análises parciais a seguir em um relatório final coeso e bem estruturado, respondendo à solicitação original do usuário. Solicitação do usuário: '%s'.\n\n--- ANÁLISES PARCIAIS ---\n%s",
                        userPrompt, combinedPartials
                );

                AIProviderRequest reduceRequest = new AIProviderRequest(reducePrompt, basePrompts.systemPrompt(), null, selectedProviderName);
                return executeAndLogTask(reduceRequest, selectedProvider, "análise de arquivo (Map-Reduce)");
            });
        }
    }

    private CompletableFuture<AIProviderResponse> executeAndLogTask(AIProviderRequest request, AIProvider provider, String taskName) {
        log.debug("Enviando requisição para a tarefa '{}' para o provedor '{}'", taskName, provider.getClass().getSimpleName());
        return provider.executeTask(request)
                .whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        log.error("!!! Erro na execução da tarefa '{}': {}", taskName, throwable.getMessage(), throwable);
                    } else {
                        log.info("<<< Resposta recebida da tarefa '{}'.", taskName);
                        log.debug("Conteúdo da resposta: {}", response.getContent());
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
