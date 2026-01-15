package com.matheusdev.mindforge.ai.provider.grok.dto;

import java.util.List;

public record GroqResponse(
    String id,
    List<Choice> choices
) {
    public record Choice(Message message) {}
    public record Message(String role, String content) {}
}
