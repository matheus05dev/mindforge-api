package com.matheusdev.mindforge.ai.provider.gemini;

import com.matheusdev.mindforge.ai.provider.AIProvider;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.ai.provider.gemini.dto.GeminiRequest;
import com.matheusdev.mindforge.ai.provider.gemini.dto.GeminiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service("geminiProvider")
@RequiredArgsConstructor
public class GeminiProvider implements AIProvider {

    private final RestTemplate restTemplate;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Override
    public AIProviderResponse executeTask(AIProviderRequest request) {
        // O modelo gemini-pro-vision é usado para multimodalidade
        String modelUrl = apiUrl.replace("gemini-pro", "gemini-pro-vision");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        List<GeminiRequest.Part> parts = new ArrayList<>();
        if (request.getTextPrompt() != null) {
            parts.add(GeminiRequest.Part.fromText(request.getTextPrompt()));
        }
        if (request.isMultimodal()) {
            String encodedString = Base64.getEncoder().encodeToString(request.getImageData());
            parts.add(GeminiRequest.Part.fromImage(request.getImageMimeType(), encodedString));
        }

        GeminiRequest.Content content = new GeminiRequest.Content(parts);
        GeminiRequest geminiRequest = new GeminiRequest(List.of(content));

        HttpEntity<GeminiRequest> entity = new HttpEntity<>(geminiRequest, headers);

        try {
            GeminiResponse response = restTemplate.postForObject(modelUrl, entity, GeminiResponse.class);

            if (response != null && !response.candidates().isEmpty() && !response.candidates().get(0).content().parts().isEmpty()) {
                String responseText = response.candidates().get(0).content().parts().get(0).text();
                return new AIProviderResponse(responseText, null);
            }
            return new AIProviderResponse(null, "A resposta do Gemini foi vazia ou inválida.");
        } catch (Exception e) {
            return new AIProviderResponse(null, "Erro ao comunicar com a API do Gemini: " + e.getMessage());
        }
    }
}
