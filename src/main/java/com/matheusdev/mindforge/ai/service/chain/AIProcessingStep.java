package com.matheusdev.mindforge.ai.service.chain;

import com.matheusdev.mindforge.ai.chat.model.ChatMessage;
import com.matheusdev.mindforge.ai.chat.model.ChatSession;
import com.matheusdev.mindforge.ai.dto.ChatRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.ai.service.model.Evidence;
import lombok.Builder;
import lombok.Data;
import lombok.With;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AIProcessingStep {
    CompletableFuture<AIContext> execute(AIContext context);

    @Data
    @Builder
    @With
    class AIContext {
        // Request Scope
        private final ChatRequest request;
        private final Long userId;

        // Session Scope
        private final ChatSession session;
        private final ChatMessage userMessage;

        // Processing State
        private final String expandedQuery;
        private final List<Evidence> evidences;
        private final String finalSystemPrompt;
        private final AIProviderResponse response;

        // Flags
        private final boolean shouldAudit;
    }
}
