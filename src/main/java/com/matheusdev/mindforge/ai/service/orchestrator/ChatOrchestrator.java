package com.matheusdev.mindforge.ai.service.orchestrator;

import com.matheusdev.mindforge.ai.dto.ChatRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.ai.service.chain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatOrchestrator {

    // Chain Steps
    private final ValidationStep validationStep;
    private final ContextRetrievalStep contextRetrievalStep;
    private final PromptBuildingStep promptBuildingStep;
    private final ExecutionStep executionStep;
    private final AuditStep auditStep;

    public CompletableFuture<AIProviderResponse> handleChatInteraction(ChatRequest chatRequest) {
        log.info(">>> [CHAT ORCHESTRATOR] Starting Chain Execution");

        // Initialize Context
        Long userId = com.matheusdev.mindforge.core.auth.util.SecurityUtils.getCurrentUserId();
        Long tenantId = com.matheusdev.mindforge.core.auth.util.SecurityUtils.getCurrentTenantId();

        AIProcessingStep.AIContext initialContext = AIProcessingStep.AIContext.builder()
                .request(chatRequest)
                .userId(userId)
                .tenantId(tenantId)
                .build();

        // Execute Chain
        return validationStep.execute(initialContext)
                .thenCompose(contextRetrievalStep::execute)
                .thenCompose(promptBuildingStep::execute)
                .thenCompose(executionStep::execute)
                .thenCompose(auditStep::execute)
                .thenApply(AIProcessingStep.AIContext::getResponse)
                .handle((response, ex) -> {
                    if (ex != null) {
                        log.error("Chain execution failed", ex);
                        AIProviderResponse errResponse = new AIProviderResponse();
                        errResponse.setContent(
                                "Desculpe, ocorreu um erro interno ao processar sua solicitação: " + ex.getMessage());
                        errResponse.setError(ex.getMessage());
                        return errResponse;
                    }
                    return response;
                });
    }
}
