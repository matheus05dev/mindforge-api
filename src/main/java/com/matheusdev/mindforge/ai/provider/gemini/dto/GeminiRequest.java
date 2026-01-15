package com.matheusdev.mindforge.ai.provider.gemini.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GeminiRequest(List<Content> contents) {

    public record Content(List<Part> parts) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Part(String text, InlineData inlineData) {
        public static Part fromText(String text) {
            return new Part(text, null);
        }

        public static Part fromImage(String mimeType, String base64Data) {
            return new Part(null, new InlineData(mimeType, base64Data));
        }
    }

    public record InlineData(
        @JsonProperty("mime_type") String mimeType,
        String data
    ) {}
}
