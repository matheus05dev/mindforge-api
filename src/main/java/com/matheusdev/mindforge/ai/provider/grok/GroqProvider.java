package com.matheusdev.mindforge.ai.provider.grok;

import com.matheusdev.mindforge.ai.provider.AIProvider;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.ai.provider.grok.dto.GroqRequest;
import com.matheusdev.mindforge.ai.provider.grok.dto.GroqResponse;
import com.matheusdev.mindforge.core.config.ResilienceConfig;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
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

    private static final String SYSTEM_INSTRUCTION = "Você é um assistente prestativo. Responda sempre em português do Brasil (pt-BR). " +
            "Use termos em inglês apenas para conceitos técnicos de programação quando estritamente necessário e, se possível, " +
            "forneça uma tradução ou explicação do termo em português. Exemplo: '...usando um *framework* (uma estrutura de trabalho)...'";

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

            List<GroqRequest.Message> messages = new ArrayList<>();
            messages.add(new GroqRequest.Message("system", SYSTEM_INSTRUCTION));
            messages.add(new GroqRequest.Message("user", request.textPrompt()));

            GroqRequest grokRequest = new GroqRequest(
                    "openai/gpt-oss-120b",
                    messages,
                    true,
                    1.0,
                    8192,
                    1.0,
                    null,
                    "medium"
            );

            HttpEntity<GroqRequest> entity = new HttpEntity<>(grokRequest, headers);

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
