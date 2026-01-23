package com.matheusdev.mindforge.ai.service.chain;

import com.matheusdev.mindforge.ai.provider.AIProvider;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.ai.service.PromptCacheService;
import com.matheusdev.mindforge.ai.service.orchestrator.RAGOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExecutionStep implements AIProcessingStep {

    private final Map<String, AIProvider> aiProviders;
    private final PromptCacheService promptCacheService;
    private final RAGOrchestrator ragOrchestrator;

    private static final String DEFAULT_PROVIDER = "ollamaProvider";

    @Override
    public CompletableFuture<AIContext> execute(AIContext context) {
        log.info(">> [CHAIN] Step 4: Execution");

        // Special handling if using RAG (delegate to RAG Orchestrator for 2-step
        // process)
        if (context.isShouldAudit() && context.getEvidences() != null && !context.getEvidences().isEmpty()) {
            log.info("Delegating execution to RAG Orchestrator due to active context.");

            String providerName = getProviderName(context.getRequest().provider());
            AIProvider provider = getProvider(providerName);

            // RAG Orchestrator handles extraction and auditing internally
            return ragOrchestrator.processWithRAG(
                    context.getSession().getDocumentId(),
                    null,
                    context.getRequest().prompt(),
                    provider,
                    providerName,
                    context.getEvidences(),
                    null // Profile could be passed if added to Context
            ).thenApply(response -> context.withResponse(response));

        } else {
            // Standard Execution
            String providerName = getProviderName(context.getRequest().provider());
            AIProvider provider = getProvider(providerName);

            AIProviderRequest request = AIProviderRequest.builder()
                    .textPrompt(context.getRequest().prompt())
                    .systemMessage(context.getFinalSystemPrompt())
                    .model(context.getRequest().model())
                    .preferredProvider(providerName)
                    .build();

            return promptCacheService.executeWithCache(provider, request).handle((response, ex) -> {
                if (ex != null) {
                    log.error("Execution error: {}", ex.getMessage());
                    throw new RuntimeException(ex);
                }
                return context.withResponse(response);
            });
        }
    }

    private String getProviderName(String provider) {
        return (provider == null || provider.isBlank()) ? DEFAULT_PROVIDER : provider;
    }

    private AIProvider getProvider(String providerName) {
        AIProvider provider = aiProviders.get(providerName);
        if (provider == null) {
            throw new IllegalArgumentException("Provedor de IA desconhecido: " + providerName);
        }
        return provider;
    }
}
