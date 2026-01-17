package com.matheusdev.mindforge.ai.provider.groq.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
    public record Message(String role, Object content) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ContentPart(
            String type,
            String text,
            @JsonProperty("image_url")
            ImageUrl imageUrl
    ) {
        public static ContentPart fromText(String text) {
            return new ContentPart("text", text, null);
        }

        public static ContentPart fromImageUrl(String url) {
            return new ContentPart("image_url", null, new ImageUrl(url));
        }
    }

    public record ImageUrl(String url) {}
}
