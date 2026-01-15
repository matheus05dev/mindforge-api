package com.matheusdev.mindforge.ai.provider.gemini.dto;

import java.util.List;

// Representa a estrutura de resposta da API do Gemini
public record GeminiResponse(List<Candidate> candidates) {
    public record Candidate(Content content) {}
    public record Content(List<Part> parts, String role) {}
    public record Part(String text) {}
}
