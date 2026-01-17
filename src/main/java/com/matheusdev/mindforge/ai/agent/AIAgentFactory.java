package com.matheusdev.mindforge.ai.agent;

import com.matheusdev.mindforge.ai.provider.ollama.dto.OllamaRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Component
public class AIAgentFactory {

    public OllamaRequest createRequest(AIAgent agent, String userPrompt, MultipartFile file) throws IOException {
        List<OllamaRequest.Message> messages = new ArrayList<>();

        // System instruction
        messages.add(new OllamaRequest.Message("system", agent.getSystemInstruction(), null));

        // User prompt and file
        List<String> images = null;
        if (file != null && !file.isEmpty()) {
            String base64Data = Base64.getEncoder().encodeToString(file.getBytes());
            images = Collections.singletonList(base64Data);
        }

        if (userPrompt != null && !userPrompt.isEmpty()) {
            messages.add(new OllamaRequest.Message("user", userPrompt, images));
        } else if (images != null) {
             messages.add(new OllamaRequest.Message("user", "", images));
        }

        // Generation config (temperature only for now in OllamaRequest)
        OllamaRequest.Options options = null;
        if (agent == AIAgent.STRUCTURED_EXTRACTOR) {
             // Ollama doesn't support responseMimeType in the same way, but we can set temperature
             options = new OllamaRequest.Options(0.9);
        }

        return OllamaRequest.builder()
                .model(agent.getModel())
                .messages(messages)
                .stream(false)
                .options(options)
                .build();
    }

    public String buildApiUrl(AIAgent agent, String baseUrl) {
        // Ollama usually has a fixed endpoint /api/chat, model is in the body
        return baseUrl;
    }
}
