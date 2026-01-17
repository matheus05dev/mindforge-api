package com.matheusdev.mindforge.ai.api;

import com.matheusdev.mindforge.ai.dto.ChatRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.ai.service.AIOrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/v1/ai/chat")
@RequiredArgsConstructor
@Tag(name = "AI Chat", description = "Endpoints for direct chat interactions with AI models")
public class ChatRestController {

    private final AIOrchestrationService aiOrchestrationService;

    @PostMapping
    @Operation(summary = "Send a prompt to the selected AI provider",
            description = "Allows sending a direct text prompt to an AI provider (e.g., Ollama, Groq) and receiving a response. You can specify the provider, model, and a system message.")
    public CompletableFuture<ResponseEntity<AIProviderResponse>> chat(@RequestBody ChatRequest chatRequest) {
        return aiOrchestrationService.handleChatInteraction(chatRequest)
                .thenApply(ResponseEntity::ok);
    }
}
