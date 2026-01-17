package com.matheusdev.mindforge.ai.agent;

import com.matheusdev.mindforge.ai.provider.gemini.dto.GeminiRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

@Component
public class AIAgentFactory {

    public GeminiRequest createRequest(AIAgent agent, String userPrompt, MultipartFile file) throws IOException {
        List<GeminiRequest.Content> contents = new ArrayList<>();

        // Para v1 API, incluir system instruction como primeira mensagem com role "user"
        List<GeminiRequest.Part> systemParts = new ArrayList<>();
        systemParts.add(GeminiRequest.Part.fromText(agent.getSystemInstruction()));
        contents.add(new GeminiRequest.Content("user", systemParts));

        // Adiciona o prompt de texto do usuário e arquivo
        List<GeminiRequest.Part> userParts = new ArrayList<>();
        if (userPrompt != null && !userPrompt.isEmpty()) {
            userParts.add(GeminiRequest.Part.fromText(userPrompt));
        }

        // Adiciona o arquivo (documento ou imagem)
        if (file != null && !file.isEmpty()) {
            String mimeType = file.getContentType();
            String base64Data = Base64.getEncoder().encodeToString(file.getBytes());
            userParts.add(GeminiRequest.Part.fromInlineData(mimeType, base64Data));
        }

        if (!userParts.isEmpty()) {
            contents.add(new GeminiRequest.Content("user", userParts));
        }

        // Define a configuração de geração (para JSON, por exemplo)
        GeminiRequest.GenerationConfig generationConfig = null;
        if (agent == AIAgent.STRUCTURED_EXTRACTOR) {
            generationConfig = new GeminiRequest.GenerationConfig("application/json", 0.9);
        }

        return GeminiRequest.builder()
                .contents(contents)
                .systemInstruction(null) // Removido para v1 API
                .generationConfig(generationConfig)
                .build();
    }

    public String buildApiUrl(AIAgent agent, String baseUrl) {
        // Constrói a URL completa usando o baseUrl e o modelo do agente
        return baseUrl + agent.getModel() + ":generateContent";
    }
}
