package com.matheusdev.mindforge.ai.provider.groq;

import com.matheusdev.mindforge.ai.provider.AIProvider;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.ai.provider.groq.dto.GroqRequest;
import com.matheusdev.mindforge.ai.provider.groq.dto.GroqResponse;
import com.matheusdev.mindforge.core.config.ResilienceConfig;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service("groqProvider")
@RequiredArgsConstructor
public class GroqProvider implements AIProvider {

    private final RestTemplate restTemplate;

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
    @CircuitBreaker(name = ResilienceConfig.AI_PROVIDER_INSTANCE, fallbackMethod = "fallback")
    @RateLimiter(name = ResilienceConfig.AI_PROVIDER_INSTANCE)
    @Retry(name = ResilienceConfig.AI_PROVIDER_INSTANCE)
    @TimeLimiter(name = ResilienceConfig.AI_PROVIDER_INSTANCE)
    public CompletableFuture<AIProviderResponse> executeTask(AIProviderRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            GroqModel selectedModel = GroqModel.fromString(request.model());

            List<GroqRequest.Message> messages = new ArrayList<>();
            messages.add(new GroqRequest.Message("system", SYSTEM_INSTRUCTION));
            messages.add(new GroqRequest.Message("user", request.textPrompt()));

            GroqRequest groqRequest = new GroqRequest(
                    selectedModel.getModelName(),
                    messages,
                    false, // Stream desativado para receber JSON completo
                    1.0,
                    selectedModel.getMaxTokens(),
                    1.0,
                    null,
                    selectedModel.getReasoningEffort()
            );

            HttpEntity<GroqRequest> entity = new HttpEntity<>(groqRequest, headers);
            GroqResponse response = restTemplate.postForObject(apiUrl, entity, GroqResponse.class);

            if (response != null && !response.choices().isEmpty()) {
                String responseText = response.choices().get(0).message().content();
                return new AIProviderResponse(responseText, null);
            }
            throw new RuntimeException("A resposta do Groq foi vazia ou inválida.");
        });
    }

    public CompletableFuture<AIProviderResponse> fallback(AIProviderRequest request, Throwable t) {
        return CompletableFuture.completedFuture(new AIProviderResponse(null, "Serviço de IA (Groq) indisponível no momento. Causa: " + t.getMessage()));
    }
}
