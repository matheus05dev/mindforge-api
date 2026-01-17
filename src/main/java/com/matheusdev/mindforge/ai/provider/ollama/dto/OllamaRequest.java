package com.matheusdev.mindforge.ai.provider.ollama.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OllamaRequest(
        String model,
        List<Message> messages,
        Boolean stream,
        Options options
) {
    public record Message(
            String role,
            String content,
            List<String> images
    ) {}

    public record Options(
            Double temperature
    ) {}
}
