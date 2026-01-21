package com.matheusdev.mindforge.ai.provider.groq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusdev.mindforge.ai.provider.AIProvider;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.ai.provider.groq.dto.GroqRequest;
import com.matheusdev.mindforge.ai.provider.groq.dto.GroqResponse;
import com.matheusdev.mindforge.ai.service.model.InteractionType;
import com.matheusdev.mindforge.core.config.ResilienceConfig;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service("groqProvider")
@RequiredArgsConstructor
@Slf4j
public class GroqProvider implements AIProvider {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final GroqTokenBudgetManager budgetManager;

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Getter
    @RequiredArgsConstructor
    public enum GroqModel {
        VERSATILE("llama-3.3-70b-versatile", 1024, null),
        INSTANT("llama-3.1-8b-instant", 1024, null),
        GPT_OSS("openai/gpt-oss-20b", 8192, "medium"),
        QWEN("qwen/qwen3-32b", 4096, "default"),
        SCOUT("meta-llama/llama-4-scout-17b-16e-instruct", 1024, null),
        MAVERICK("meta-llama/llama-4-maverick-17b-128e-instruct", 1024, null);

        private final String modelName;
        private final int maxTokens;
        private final String reasoningEffort;

        public static GroqModel fromString(String model) {
            if (!StringUtils.hasText(model)) {
                return INSTANT; // Modelo padrão caso nenhum seja fornecido
            }
            return Arrays.stream(values())
                    .filter(groqModel -> groqModel.name().equalsIgnoreCase(model))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Modelo Groq desconhecido: " + model));
        }
    }

    private static final String SYSTEM_INSTRUCTION = "Você é um assistente prestativo. Responda sempre em português do Brasil (pt-BR).";

    @Override
    @CircuitBreaker(name = ResilienceConfig.GROQ_INSTANCE, fallbackMethod = "fallback")
    @RateLimiter(name = ResilienceConfig.GROQ_INSTANCE)
    @Retry(name = ResilienceConfig.GROQ_INSTANCE)
    @TimeLimiter(name = ResilienceConfig.GROQ_INSTANCE)
    public CompletableFuture<AIProviderResponse> executeTask(AIProviderRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validar budget antes de fazer a requisição
                GroqModel selectedModel = GroqModel.fromString(request.model());
                int estimatedTokens = estimateTokens(SYSTEM_INSTRUCTION, request.textPrompt(),
                        selectedModel.getMaxTokens());

                if (!budgetManager.canConsume(estimatedTokens)) {
                    int available = budgetManager.getAvailableBudget();
                    throw new GroqBudgetExceededException(available, estimatedTokens);
                }
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + apiKey);

                List<GroqRequest.Message> messages = new ArrayList<>();
                String systemContent = StringUtils.hasText(request.systemMessage())
                        ? request.systemMessage()
                        : SYSTEM_INSTRUCTION;
                messages.add(new GroqRequest.Message("system", systemContent));

                Object messageContent;
                if (request.multimodal() && request.imageData() != null) {
                    List<GroqRequest.ContentPart> contentParts = new ArrayList<>();
                    contentParts.add(GroqRequest.ContentPart.fromText(request.textPrompt()));

                    String base64Image = Base64.getEncoder().encodeToString(request.imageData());
                    String dataUri = "data:" + request.imageMimeType() + ";base64," + base64Image;
                    contentParts.add(GroqRequest.ContentPart.fromImageUrl(dataUri));

                    messageContent = contentParts;
                } else {
                    messageContent = request.textPrompt();
                }
                messages.add(new GroqRequest.Message("user", messageContent));

                double temperature = (request.temperature() != null && request.temperature() > 0)
                        ? request.temperature()
                        : 0.7; // Default seguro (era 1.0)

                int effectiveMaxTokens = (request.maxTokens() != null && request.maxTokens() > 0)
                        ? request.maxTokens()
                        : selectedModel.getMaxTokens();

                GroqRequest groqRequest = new GroqRequest(
                        selectedModel.getModelName(),
                        messages,
                        false,
                        temperature, // Usando temperatura dinâmica
                        effectiveMaxTokens,
                        1.0,
                        null,
                        selectedModel.getReasoningEffort());

                // --- LOG DE DEBUG: MOSTRA O QUE ESTÁ SENDO ENVIADO ---
                String requestJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(groqRequest);
                log.info(
                        "\n================= [GROQ REQUEST] =================\nURL: {}\nPayload:\n{}\n==================================================",
                        apiUrl, requestJson);
                // -----------------------------------------------------

                HttpEntity<GroqRequest> entity = new HttpEntity<>(groqRequest, headers);
                GroqResponse response = restTemplate.postForObject(apiUrl, entity, GroqResponse.class);

                // --- LOG DE DEBUG: MOSTRA O QUE FOI RECEBIDO ---
                String responseJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
                log.info(
                        "\n================= [GROQ RESPONSE] =================\nPayload:\n{}\n===================================================",
                        responseJson);
                // ---------------------------------------------------

                if (response != null && !response.choices().isEmpty()) {
                    String responseText = response.choices().get(0).message().content();

                    // Registrar uso de tokens (Real ou Estimado)
                    int totalTokens = 0;
                    if (response.usage() != null && response.usage().totalTokens() > 0) {
                        totalTokens = response.usage().totalTokens();
                    } else {
                        // Fallback: estimativa se a API retornar 0 (comum em alguns tiers/modelos)
                        int inputLength = (request.textPrompt() != null ? request.textPrompt().length() : 0) +
                                (request.systemMessage() != null ? request.systemMessage().length() : 0);
                        int outputLength = responseText != null ? responseText.length() : 0;
                        totalTokens = (inputLength + outputLength) / 4;
                        log.info("⚠️ Groq API reportou 0 tokens. Usando estimativa baseada em caracteres: ~{} tokens.",
                                totalTokens);
                    }

                    budgetManager.recordUsage(totalTokens);

                    return new AIProviderResponse(responseText, null, null, null, null);
                }
                throw new RuntimeException("A resposta do Groq foi vazia ou inválida.");

            } catch (JsonProcessingException e) {
                log.error("Erro ao serializar JSON para debug", e);
                throw new RuntimeException("Erro interno de serialização JSON", e);
            } catch (Exception e) {
                log.error("Erro ao comunicar com Groq: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
                throw new RuntimeException("Erro na comunicação com Groq", e);
            }
        });
    }

    public CompletableFuture<AIProviderResponse> fallback(AIProviderRequest request, Throwable t) {
        log.error("!!! ALERTA !!! Serviço de IA (Groq) indisponível ou falhou. Causa: {}", t.getMessage());
        String errorMessage = "Desculpe, não foi possível processar sua solicitação no momento devido a uma instabilidade no serviço de IA (Groq). Erro: "
                + t.getMessage();
        return CompletableFuture.completedFuture(
                new AIProviderResponse(errorMessage, null, errorMessage, null, InteractionType.SYSTEM));
    }

    /**
     * Estima a quantidade de tokens que uma requisição consumirá.
     * Usa aproximação conservadora: ~4 caracteres = 1 token.
     *
     * @param systemMessage Mensagem de sistema
     * @param userMessage   Mensagem do usuário
     * @param maxTokens     Máximo de tokens de resposta
     * @return Estimativa total de tokens (prompt + resposta + margem)
     */
    private int estimateTokens(String systemMessage, String userMessage, int maxTokens) {
        int systemChars = systemMessage != null ? systemMessage.length() : 0;
        int userChars = userMessage != null ? userMessage.length() : 0;

        // Estimativa conservadora: 4 chars ≈ 1 token
        int promptTokens = (systemChars + userChars) / 4;

        // Total = prompt + resposta máxima + margem de segurança (10%)
        int estimated = promptTokens + maxTokens + (int) (maxTokens * 0.1);

        log.debug("📊 Estimativa de tokens: prompt={}, maxResponse={}, total={}",
                promptTokens, maxTokens, estimated);

        return estimated;
    }
}
