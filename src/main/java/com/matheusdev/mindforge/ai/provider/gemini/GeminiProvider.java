package com.matheusdev.mindforge.ai.provider.gemini;

import com.matheusdev.mindforge.ai.provider.AIProvider;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.ai.provider.gemini.dto.GeminiRequest;
import com.matheusdev.mindforge.ai.provider.gemini.dto.GeminiResponse;
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

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service("geminiProvider")
@RequiredArgsConstructor
public class GeminiProvider implements AIProvider {

    private final RestTemplate restTemplate;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Override
    @CircuitBreaker(name = ResilienceConfig.AI_PROVIDER_INSTANCE, fallbackMethod = "fallback")
    @RateLimiter(name = ResilienceConfig.AI_PROVIDER_INSTANCE)
    @Retry(name = ResilienceConfig.AI_PROVIDER_INSTANCE)
    @TimeLimiter(name = ResilienceConfig.AI_PROVIDER_INSTANCE)
    public CompletableFuture<AIProviderResponse> executeTask(AIProviderRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            String fullApiUrl = apiUrl + "&key=" + apiKey;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            GeminiRequest.Part textPart = GeminiRequest.Part.fromText(request.textPrompt());
            List<GeminiRequest.Part> parts;

            if (request.multimodal()) {
                String base64ImageData = Base64.getEncoder().encodeToString(request.imageData());
                GeminiRequest.Part imagePart = GeminiRequest.Part.fromImage(request.imageMimeType(), base64ImageData);
                parts = List.of(textPart, imagePart);
            } else {
                parts = Collections.singletonList(textPart);
            }

            GeminiRequest.Content content = new GeminiRequest.Content(parts);
            GeminiRequest geminiRequest = new GeminiRequest(Collections.singletonList(content));
            HttpEntity<GeminiRequest> entity = new HttpEntity<>(geminiRequest, headers);

            GeminiResponse response = restTemplate.postForObject(fullApiUrl, entity, GeminiResponse.class);

            if (response != null && !response.candidates().isEmpty()) {
                String responseText = response.candidates().get(0).content().parts().get(0).text();
                return new AIProviderResponse(responseText, null);
            }
            throw new RuntimeException("A resposta do Gemini foi vazia ou inválida.");
        });
    }

    public CompletableFuture<AIProviderResponse> fallback(AIProviderRequest request, Throwable t) {
        return CompletableFuture.completedFuture(new AIProviderResponse(null, "Serviço de IA (Gemini) indisponível no momento. Causa: " + t.getMessage()));
    }
}
