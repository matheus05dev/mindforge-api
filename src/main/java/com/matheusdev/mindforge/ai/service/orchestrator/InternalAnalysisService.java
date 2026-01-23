package com.matheusdev.mindforge.ai.service.orchestrator;

import com.matheusdev.mindforge.ai.provider.AIProvider;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.ai.service.PromptCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class InternalAnalysisService {

    private final Map<String, AIProvider> aiProviders;
    private final PromptCacheService promptCacheService;

    private static final String DEFAULT_PROVIDER = "ollamaProvider";

    /**
     * Utilitário para obter provedor ou lançar erro.
     */
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

    /**
     * Executa task com log e fallback (Similar ao método privado original do
     * AIOrchestrationService).
     * Pode ser movido para um BaseService ou utilitário se reutilizado.
     */
    private CompletableFuture<AIProviderResponse> executeAndLogTask(AIProviderRequest request, AIProvider provider,
            String taskName) {
        log.debug("Enviando requisição '{}' para o provedor '{}'", taskName, provider.getClass().getSimpleName());

        return promptCacheService.executeWithCache(provider, request)
                .handle((response, throwable) -> {
                    if (throwable != null) {
                        Throwable cause = throwable;
                        while (cause.getCause() != null && cause != cause.getCause()) {
                            cause = cause.getCause();
                        }

                        boolean isOllama = provider.getClass().getSimpleName().toLowerCase().contains("ollama");

                        if (!isOllama) {
                            log.warn("⚠️ Falha no provedor principal '{}'. Erro: {}. Iniciando FALLBACK para Ollama...",
                                    taskName, cause.getMessage());
                            AIProvider ollama = getProvider("ollamaProvider");
                            try {
                                return promptCacheService.executeWithCache(ollama, request).join();
                            } catch (Exception e) {
                                log.error("❌ Falha crítica no Fallback (Ollama) para '{}': {}", taskName,
                                        e.getMessage());
                                throw new RuntimeException("Falha no provedor principal e no fallback", e);
                            }
                        }

                        log.error("!!! ERRO na execução da tarefa '{}': {}", taskName, cause.getMessage(), cause);
                        throw new RuntimeException(cause);
                    } else {
                        log.info("<<< SUCESSO na tarefa '{}'. Resposta recebida.", taskName);
                        return response;
                    }
                });
    }

    /**
     * Executa uma análise interna sem interação direta do usuário (headless).
     */
    public CompletableFuture<AIProviderResponse> executeInternalAnalysis(String prompt, String systemMessage) {
        log.info(">>> [INTERNAL] Executando análise interna (headless)...");
        String providerName = DEFAULT_PROVIDER;
        AIProvider selectedProvider = getProvider(providerName);

        AIProviderRequest request = AIProviderRequest.builder()
                .textPrompt(prompt)
                .systemMessage(systemMessage)
                .preferredProvider(providerName)
                .build();

        return executeAndLogTask(request, selectedProvider, "internal-analysis");
    }

    /**
     * Gera um resumo "TL;DR" inteligente.
     */
    public CompletableFuture<String> summarizeContent(String content) {
        log.info(">>> [INTERNAL] Gerando Auto-TL;DR para conteúdo de {} chars.", content.length());

        String systemPrompt = "Você é um especialista em síntese de informação. Sua missão é gerar um 'TL;DR' (Too Long; Didn't Read) perfeito.\n"
                + "Regras:\n" +
                "1. Seja extremamente conciso (máximo 3-4 frases).\n" +
                "2. Capture a essência e o 'porquê' do documento/texto.\n" +
                "3. Use tópicos se facilitar a leitura.\n" +
                "4. Responda APENAS o resumo, sem introduções.";

        String userPrompt = "Resuma o seguinte conteúdo:\n\n" +
                (content.length() > 10000 ? content.substring(0, 10000) + "... (truncado)" : content);

        return executeInternalAnalysis(userPrompt, systemPrompt)
                .thenApply(AIProviderResponse::getContent);
    }
}
