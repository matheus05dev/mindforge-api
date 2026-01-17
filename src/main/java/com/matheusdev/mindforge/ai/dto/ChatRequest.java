package com.matheusdev.mindforge.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ChatRequest(
        @Schema(description = "The text prompt to send to the AI.", required = true, example = "Explique o que é a teoria da relatividade em termos simples.")
        String prompt,

        @Schema(description = "The AI provider to use. Options: 'ollamaProvider', 'groqProvider'. Defaults to 'ollamaProvider' if not specified.", example = "ollamaProvider")
        String provider,

        @Schema(description = "The specific model to use (optional, depends on the provider).", example = "llama3")
        String model,

        @Schema(description = "A system message or instruction to guide the AI's behavior (optional).", example = "Seja conciso e direto.")
        String systemMessage
) {
}
