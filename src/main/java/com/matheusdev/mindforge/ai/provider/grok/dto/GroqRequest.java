package com.matheusdev.mindforge.ai.provider.grok.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GroqRequest(
        String model,
        List<Message> messages,
        boolean stream,
        Double temperature,
        @JsonProperty("max_completion_tokens") Integer maxCompletionTokens,
        @JsonProperty("top_p") Double topP,
        String stop,
        @JsonProperty("reasoning_effort") String reasoningEffort
) {
    public record Message(String role, String content) {
    }
}
