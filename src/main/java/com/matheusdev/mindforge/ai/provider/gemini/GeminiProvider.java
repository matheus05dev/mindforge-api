package com.matheusdev.mindforge.ai.provider.gemini;

import com.matheusdev.mindforge.ai.provider.AIProvider;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.ai.provider.gemini.dto.GeminiRequest;
import com.matheusdev.mindforge.ai.provider.gemini.dto.GeminiResponse;
import com.matheusdev.mindforge.ai.provider.groq.GroqProvider;
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

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service("geminiProvider")
@RequiredArgsConstructor
public class GeminiProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiProvider.class);

    private final RestTemplate restTemplate;
    private final GroqProvider groqProvider;

    @Value("${gemini.api.key}")
    private String apiKey;

    // BUSCAR DO PROPERTIES (Remova as Strings hardcoded antigas)
    @Value("${gemini.api.url.text}")
    private String textApiUrl;

    @Value("${gemini.api.url.multimodal}")
    private String multimodalApiUrl;

    @Override
    @CircuitBreaker(name = ResilienceConfig.AI_PROVIDER_INSTANCE, fallbackMethod = "fallback")
    @RateLimiter(name = ResilienceConfig.AI_PROVIDER_INSTANCE)
    @Retry(name = ResilienceConfig.AI_PROVIDER_INSTANCE)
    @TimeLimiter(name = ResilienceConfig.AI_PROVIDER_INSTANCE)
    public CompletableFuture<AIProviderResponse> executeTask(AIProviderRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            GeminiRequest geminiRequest = buildGeminiRequest(request);

            // Usar as variáveis injetadas
            String apiUrl = request.multimodal() ? multimodalApiUrl : textApiUrl;

            log.debug("Chamando Gemini API em: {}", apiUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-goog-api-key", apiKey);

            HttpEntity<GeminiRequest> entity = new HttpEntity<>(geminiRequest, headers);

            // O RestTemplate fará a chamada para a URL do seu .properties (v1/models/gemini-1.5-flash)
            GeminiResponse response = restTemplate.postForObject(apiUrl, entity, GeminiResponse.class);

            if (response != null && response.candidates() != null && !response.candidates().isEmpty()) {
                String responseText = response.candidates().get(0).content().parts().get(0).text();
                return new AIProviderResponse(responseText, null);
            }
            throw new RuntimeException("A resposta do Gemini foi vazia ou inválida.");
        });
    }

    private GeminiRequest buildGeminiRequest(AIProviderRequest request) {
        List<GeminiRequest.Part> parts = new ArrayList<>();
        parts.add(GeminiRequest.Part.fromText(request.textPrompt()));

        if (request.multimodal() && request.imageData() != null) {
            String base64Image = Base64.getEncoder().encodeToString(request.imageData());
            parts.add(GeminiRequest.Part.fromInlineData(request.imageMimeType(), base64Image));
        }

        GeminiRequest.Content userContent = new GeminiRequest.Content("user", parts);
        GeminiRequest.SystemInstruction systemInstruction = null;
        if (request.systemMessage() != null) {
            systemInstruction = new GeminiRequest.SystemInstruction(
                    Collections.singletonList(GeminiRequest.Part.fromText(request.systemMessage()))
            );
        }

        return GeminiRequest.builder()
                .contents(Collections.singletonList(userContent))
                .systemInstruction(systemInstruction)
                .build();
    }

    public CompletableFuture<AIProviderResponse> fallback(AIProviderRequest request, Throwable t) {
        log.warn("Serviço de IA (Gemini) indisponível. Acionando fallback para Groq. Causa: {}", t.getMessage());
        return groqProvider.executeTask(request);
    }
}
