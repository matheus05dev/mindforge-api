package com.matheusdev.mindforge.ai.provider.ollama;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusdev.mindforge.ai.provider.AIProvider;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.ai.provider.groq.GroqProvider;
import com.matheusdev.mindforge.ai.provider.ollama.dto.OllamaRequest;
import com.matheusdev.mindforge.ai.provider.ollama.dto.OllamaResponse;
import com.matheusdev.mindforge.core.config.ResilienceConfig;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementação do provedor de IA usando Ollama (Local ou Remoto).
 * Esta classe gerencia a comunicação HTTP com a API do Ollama.
 */
@Service("ollamaProvider")
@RequiredArgsConstructor
public class OllamaProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(OllamaProvider.class);

    private final RestTemplate restTemplate;
    private final GroqProvider groqProvider;
    private final ObjectMapper objectMapper; // Usado para logar o JSON da requisição/resposta

    @Value("${ollama.api.url}")
    private String apiUrl;

    @Value("${ollama.model}")
    private String model;

    /**
     * Executa uma tarefa de IA (Chat ou Multimodal) de forma assíncrona.
     * Aplica padrões de resiliência (Circuit Breaker, Retry, etc.).
     */
    @Override
    @CircuitBreaker(name = ResilienceConfig.OLLAMA_INSTANCE, fallbackMethod = "fallback")
    @RateLimiter(name = ResilienceConfig.OLLAMA_INSTANCE)
    @Retry(name = ResilienceConfig.OLLAMA_INSTANCE)
    @TimeLimiter(name = ResilienceConfig.OLLAMA_INSTANCE)
    public CompletableFuture<AIProviderResponse> executeTask(AIProviderRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Constrói o objeto de requisição específico do Ollama
                OllamaRequest ollamaRequest = buildOllamaRequest(request);

                // --- LOG DE DEBUG: MOSTRA O QUE ESTÁ SENDO ENVIADO ---
                String requestJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(ollamaRequest);
                log.info(
                        "\n================= [OLLAMA REQUEST] =================\nURL: {}\nPayload:\n{}\n====================================================",
                        apiUrl, requestJson);
                // -----------------------------------------------------

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<OllamaRequest> entity = new HttpEntity<>(ollamaRequest, headers);

                // 2. Faz a chamada HTTP POST
                OllamaResponse response = restTemplate.postForObject(apiUrl, entity, OllamaResponse.class);

                // --- LOG DE DEBUG: MOSTRA O QUE FOI RECEBIDO ---
                String responseJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
                log.info(
                        "\n================= [OLLAMA RESPONSE] =================\nPayload:\n{}\n=====================================================",
                        responseJson);
                // ---------------------------------------------------

                if (response != null && response.message() != null) {
                    log.info("✅ [OLLAMA] Resposta recebida com sucesso! Tamanho da resposta: {} caracteres",
                            response.message().content() != null ? response.message().content().length() : 0);
                    return new AIProviderResponse(response.message().content(), null, null, null, null);
                }

                log.error("Resposta do Ollama veio nula ou sem mensagem.");
                throw new RuntimeException("A resposta do Ollama foi vazia ou inválida.");

            } catch (JsonProcessingException e) {
                log.error("Erro ao serializar JSON para debug", e);
                throw new RuntimeException("Erro interno de serialização JSON", e);
            } catch (org.springframework.web.client.ResourceAccessException e) {
                log.error("Erro de conexão com Ollama em {}: {}. Verifique se o Ollama está rodando.", apiUrl,
                        e.getMessage());
                throw new RuntimeException(
                        "Não foi possível conectar ao Ollama. Verifique se o serviço está rodando em " + apiUrl, e);
            } catch (org.springframework.web.client.HttpClientErrorException
                    | org.springframework.web.client.HttpServerErrorException e) {
                log.error("Erro HTTP ao comunicar com Ollama: Status {} - {}", e.getStatusCode(),
                        e.getResponseBodyAsString());
                throw new RuntimeException("Erro HTTP ao comunicar com Ollama: " + e.getStatusCode(), e);
            } catch (Exception e) {
                log.error("Erro ao comunicar com Ollama: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
                throw e;
            }
        });
    }

    @Value("${ollama.model.vision:qwen3-vl:4b}")
    private String visionModel;

    /**
     * Constrói o objeto de requisição (DTO) para o formato esperado pelo Ollama.
     */
    private OllamaRequest buildOllamaRequest(AIProviderRequest request) {
        List<OllamaRequest.Message> messages = new ArrayList<>();

        // Adiciona mensagem de sistema se houver
        if (request.systemMessage() != null) {
            messages.add(new OllamaRequest.Message("system", request.systemMessage(), null));
        }

        // Trata imagens para requisições multimodais
        List<String> images = null;
        if (request.multimodal() && request.imageData() != null) {
            String base64Image = Base64.getEncoder().encodeToString(request.imageData());
            images = Collections.singletonList(base64Image);
            log.debug("Imagem convertida para Base64 (tamanho: {} chars)", base64Image.length());
        }

        // Adiciona a mensagem do usuário
        messages.add(new OllamaRequest.Message("user", request.textPrompt(), images));

        // Seleciona o modelo adequado (Texto puro vs Visão)
        String selectedModel = (request.multimodal() && images != null && !images.isEmpty())
                ? visionModel
                : model;

        if (!selectedModel.equals(model)) {
            log.info("👁️ Modo Multimodal detectado: Usando modelo de visão '{}'", selectedModel);
        }

        // Retorna o objeto construído
        return OllamaRequest.builder()
                .model(selectedModel)
                .messages(messages)
                .stream(false) // Desativa streaming para simplificar o tratamento
                .options(new OllamaRequest.Options(request.temperature() != null ? request.temperature() : 0.7))
                .build();
    }

    /**
     * Método de fallback caso o Ollama falhe.
     * Redireciona a chamada para o GroqProvider se houver budget disponível.
     */
    public CompletableFuture<AIProviderResponse> fallback(AIProviderRequest request, Throwable t) {
        log.warn("!!! ALERTA !!! Serviço de IA (Ollama) indisponível ou falhou. Causa: {}", t.getMessage());

        // Log genérico para indicar que o fallback está ocorrendo
        if (request.systemMessage() != null && !request.systemMessage().isEmpty()) {
            log.info("Request com contexto (systemMessage presente) sendo redirecionado para o backup.");
        }

        try {
            log.info("Redirecionando requisição para o provedor de backup: GroqProvider.");
            return groqProvider.executeTask(request);
        } catch (com.matheusdev.mindforge.ai.provider.groq.GroqBudgetExceededException budgetEx) {
            log.warn("⚠️ Groq backup indisponível: {}", budgetEx.getMessage());
            String errorMessage = "Ollama timeout e Groq sem budget disponível. Aguarde 1 minuto ou tente novamente.";
            return CompletableFuture.completedFuture(
                    new AIProviderResponse(errorMessage, null, errorMessage, null, null));
        }
    }
}
