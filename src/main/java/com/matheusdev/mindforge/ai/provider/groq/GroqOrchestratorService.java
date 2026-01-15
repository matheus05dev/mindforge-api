package com.matheusdev.mindforge.ai.provider.groq;

import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class GroqOrchestratorService {

    private static final Logger logger = LoggerFactory.getLogger(GroqOrchestratorService.class);

    private final GroqProvider groqProvider;

    /**
     * Orquestra uma requisição para a Groq, com lógica de fallback entre modelos.
     * Tenta primeiro com um modelo preferencial e, em caso de falha, tenta com um modelo secundário.
     *
     * @param prompt O texto do prompt para a IA.
     * @param preferredModel O modelo preferencial (ex: "VERSATILE", "QWEN").
     * @param fallbackModel O modelo de fallback (ex: "INSTANT").
     * @return Um CompletableFuture com a resposta da IA.
     */
    public CompletableFuture<AIProviderResponse> executeWithFallback(String prompt, String preferredModel, String fallbackModel) {
        logger.info("Iniciando orquestração Groq. Modelo preferencial: {}", preferredModel);

        // Usando o construtor de 4 argumentos para evitar ambiguidade
        AIProviderRequest initialRequest = new AIProviderRequest(prompt, null, preferredModel, null);

        return groqProvider.executeTask(initialRequest)
                .handle((response, throwable) -> {
                    // CORREÇÃO: Usando getContent() do Lombok e verificando o conteúdo
                    if (throwable == null && response != null && response.getContent() != null && !response.getContent().isEmpty()) {
                        logger.info("Sucesso com o modelo preferencial: {}", preferredModel);
                        return CompletableFuture.completedFuture(response);
                    }

                    // Se a primeira chamada falhou
                    logger.warn("Falha ao usar o modelo preferencial '{}'. Causa: {}. Tentando com o modelo de fallback '{}'.",
                            preferredModel, (throwable != null ? throwable.getMessage() : "Resposta inválida"), fallbackModel);

                    // Cria e executa a requisição para o modelo de fallback
                    AIProviderRequest fallbackRequest = new AIProviderRequest(prompt, null, fallbackModel, null);
                    return groqProvider.executeTask(fallbackRequest);
                })
                .thenCompose(completableFuture -> completableFuture);
    }

    /**
     * Executa uma tarefa com um único modelo, sem fallback.
     *
     * @param prompt O texto do prompt.
     * @param model O modelo a ser usado.
     * @return Um CompletableFuture com a resposta da IA.
     */
    public CompletableFuture<AIProviderResponse> execute(String prompt, String model) {
        logger.info("Executando tarefa simples com o modelo Groq: {}", model);
        // Usando o construtor de 4 argumentos para evitar ambiguidade
        AIProviderRequest request = new AIProviderRequest(prompt, null, model, null);
        return groqProvider.executeTask(request);
    }
}
