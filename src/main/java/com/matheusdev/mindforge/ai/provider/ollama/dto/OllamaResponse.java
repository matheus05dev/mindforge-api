package com.matheusdev.mindforge.ai.provider.ollama.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaResponse(
        String model,
        @JsonProperty("created_at")
        String createdAt,
        Message message,
        Boolean done
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(
            String role,
            String content
    ) {}
}
