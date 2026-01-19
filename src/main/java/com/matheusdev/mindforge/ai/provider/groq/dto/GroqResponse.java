package com.matheusdev.mindforge.ai.provider.groq.dto;

import java.util.List;

public record GroqResponse(
        String id,
        List<Choice> choices,
        Usage usage) {
    public record Choice(Message message) {
    }

    public record Message(String role, String content) {
    }

    public record Usage(
            int promptTokens,
            int completionTokens,
            int totalTokens) {
    }
}
