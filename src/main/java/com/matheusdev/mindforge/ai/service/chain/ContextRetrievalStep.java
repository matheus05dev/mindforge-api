package com.matheusdev.mindforge.ai.service.chain;

import com.matheusdev.mindforge.ai.service.DocumentAnalyzer;
import com.matheusdev.mindforge.ai.service.RAGService;
import com.matheusdev.mindforge.ai.service.VectorStoreService;
import com.matheusdev.mindforge.ai.service.model.Evidence;
import com.matheusdev.mindforge.ai.service.orchestrator.RAGOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContextRetrievalStep implements AIProcessingStep {

    private final RAGService ragService;
    private final VectorStoreService vectorStoreService;
    private final RAGOrchestrator ragOrchestrator;

    @Override
    public CompletableFuture<AIContext> execute(AIContext context) {
        return CompletableFuture.supplyAsync(() -> {
            log.info(">> [CHAIN] Step 2: Context Retrieval");

            if (!StringUtils.hasText(context.getSession().getDocumentId())) {
                log.info("Sem documento vinculado. Pulando RAG.");
                return context;
            }

            String docId = context.getSession().getDocumentId();
            String userPrompt = context.getRequest().prompt();

            // Layer 3: Discovery & Pre-Analysis
            DocumentAnalyzer.DocumentProfile docProfile = vectorStoreService.getDocumentProfile(docId);

            String expandedQuery = ragOrchestrator.expandQueryWithDynamicTerms(userPrompt, docProfile);
            if (!expandedQuery.equals(userPrompt)) {
                log.info("🔍 Query Expandida: '{}' -> '{}'", userPrompt, expandedQuery);
            }

            List<Evidence> evidences = ragService.processQueryWithRAG(docId, null, expandedQuery, 8);

            return context.withExpandedQuery(expandedQuery)
                    .withEvidences(evidences)
                    .withShouldAudit(true); // Enable audit step later if RAG was used
        });
    }
}
