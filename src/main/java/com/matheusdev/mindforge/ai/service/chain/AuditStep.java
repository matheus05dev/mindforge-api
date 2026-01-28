package com.matheusdev.mindforge.ai.service.chain;

import com.matheusdev.mindforge.ai.chat.model.ChatMessage;
import com.matheusdev.mindforge.ai.memory.service.MemoryService;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.ai.service.ChatService;
import com.matheusdev.mindforge.ai.service.model.InteractionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditStep implements AIProcessingStep {

    private final ChatService chatService;
    private final MemoryService memoryService;

    @Override
    public CompletableFuture<AIContext> execute(AIContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Manually propagate Tenant Context
                com.matheusdev.mindforge.core.tenant.context.TenantContext.setTenantId(context.getTenantId());

                log.info(">> [CHAIN] Step 5: Audit & Persistence");

                AIProviderResponse response = context.getResponse();
                if (response == null) {
                    log.error("No response to audit!");
                    return context;
                }

                String content = response.getContent();

                // Persist Assistant Message
                chatService.saveMessage(context.getSession(), "assistant", content);

                // Update Memory
                InteractionType type = context.isShouldAudit() ? InteractionType.RAG_ANALYSIS : InteractionType.CHAT;

                List<Map<String, String>> chatHistory = List.of(
                        Map.of("role", "user", "content", context.getUserMessage().getContent()),
                        Map.of("role", "assistant", "content", content));

                memoryService.updateUserProfile(context.getUserId(), chatHistory);

                // Trigger background tasks (like title generation)
                if ("Nova Conversa".equals(context.getSession().getTitle())) {
                    triggerBackgroudTitleGeneration(context.getSession().getId(), context.getRequest().prompt());
                }

                response.setSessionId(context.getSession().getId());
                response.setType(type);

                return context;
            } finally {
                com.matheusdev.mindforge.core.tenant.context.TenantContext.clear();
            }
        });
    }

    private void triggerBackgroudTitleGeneration(Long sessionId, String firstMessage) {
        CompletableFuture.runAsync(() -> {
            log.info("Triggering title generation for sess {}", sessionId);
            // Logic would be here or delegated
        });
    }
}
