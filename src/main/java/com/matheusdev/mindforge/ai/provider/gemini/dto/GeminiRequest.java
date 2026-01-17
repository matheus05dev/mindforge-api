package com.matheusdev.mindforge.ai.provider.gemini.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeminiRequest(
        List<Content> contents,
        @JsonProperty("systemInstruction")
        SystemInstruction systemInstruction,
        @JsonProperty("generationConfig")
        GenerationConfig generationConfig
) {

    public record Content(String role, List<Part> parts) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Part(
            String text,
            @JsonProperty("inlineData")
            InlineData inlineData
    ) {
        public static Part fromText(String text) {
            return new Part(text, null);
        }

        public static Part fromInlineData(String mimeType, String base64Data) {
            return new Part(null, new InlineData(mimeType, base64Data));
        }
    }

    public record InlineData(
            @JsonProperty("mimeType") String mimeType,
            String data
    ) {}

    public record SystemInstruction(List<Part> parts) {}

    public record GenerationConfig(
            @JsonProperty("responseMimeType") String responseMimeType,
            Double temperature
    ) {}
}
