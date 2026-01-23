package com.matheusdev.mindforge.ai.service.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusdev.mindforge.ai.service.model.Answer;
import com.matheusdev.mindforge.ai.service.model.AuditedAnswer;
import com.matheusdev.mindforge.ai.service.model.Evidence;
import com.matheusdev.mindforge.ai.service.model.EvidenceRef;
import com.matheusdev.mindforge.ai.service.model.InteractionType;
import com.matheusdev.mindforge.ai.dto.PromptPair;
import com.matheusdev.mindforge.ai.provider.AIProvider;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.ai.service.DocumentAnalyzer;
import com.matheusdev.mindforge.ai.service.PromptBuilderService;
import com.matheusdev.mindforge.ai.service.PromptCacheService;
import dev.langchain4j.data.document.Document;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class RAGOrchestrator {

    private final Map<String, AIProvider> aiProviders;
    private final PromptBuilderService promptBuilderService;
    private final PromptCacheService promptCacheService;
    private final DocumentAnalyzer documentAnalyzer;
    private final ObjectMapper objectMapper;

    private static final String DEFAULT_PROVIDER = "ollamaProvider";
    private static final int MAX_EVIDENCE_CHARS = 12000;

    private AIProvider getProvider(String providerName) {
        AIProvider provider = aiProviders.get(providerName);
        if (provider == null) {
            throw new IllegalArgumentException("Provedor de IA desconhecido: " + providerName);
        }
        return provider;
    }

    private CompletableFuture<AIProviderResponse> executeAndLogTask(AIProviderRequest request, AIProvider provider,
            String taskName) {
        // Simplified execution wrapper - ideally shared
        return promptCacheService.executeWithCache(provider, request).handle((response, ex) -> {
            if (ex != null) {
                log.error("Erro em RAG task {}: {}", taskName, ex.getMessage());
                throw new RuntimeException(ex);
            }
            return response;
        });
    }

    // --- Public Methods ---

    public CompletableFuture<AIProviderResponse> processWithRAG(
            String documentId,
            Document document, // Optional if strictly ID based
            String userPrompt,
            AIProvider provider, // Optional override
            String providerName,
            List<Evidence> evidences,
            DocumentAnalyzer.DocumentProfile docProfile) {

        log.info("🔍 Processamento RAG: Iniciando nova estratégia de 2 etapas.");

        String targetProviderName = (providerName != null) ? providerName : DEFAULT_PROVIDER;
        AIProvider targetProvider = (provider != null) ? provider : getProvider(targetProviderName);

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (evidences == null || evidences.isEmpty()) {
                    log.warn("Nenhum segmento relevante encontrado. Retornando resposta padrão.");
                    return new AIProviderResponse(getNotFoundJson(), null, "RAG fallback", null,
                            InteractionType.RAG_ANALYSIS);
                }

                String evidenceText = formatEvidences(evidences);

                // Auditor Prompt
                PromptPair ragPrompts = promptBuilderService.buildAuditorPrompt(userPrompt, evidenceText);
                String extractionPrompt = ragPrompts.userPrompt();

                String baseSystemPrompt = ragPrompts.systemPrompt();
                String systemPrompt = enrichSystemPromptWithGlossary(baseSystemPrompt, userPrompt, docProfile);

                AIProviderRequest extractionRequest = AIProviderRequest.builder()
                        .textPrompt(extractionPrompt)
                        .systemMessage(systemPrompt)
                        .preferredProvider(targetProviderName)
                        .temperature(0.2)
                        .build();

                log.info("📤 Etapa 1: Enviando prompt de extração para '{}'.", targetProviderName);
                AIProviderResponse extractionResponse = executeAndLogTask(extractionRequest, targetProvider,
                        "Extração RAG").get();
                String extractedContent = extractionResponse.getContent();

                log.info("✅ Etapa 1: Resposta de extração recebida.");

                AuditedAnswer finalAnswer;
                boolean isEssentiallyEmpty = extractedContent == null || extractedContent.isBlank();
                boolean isStrictlyNotFound = extractedContent != null &&
                        (extractedContent.trim().equalsIgnoreCase("INFORMAÇÃO NÃO ENCONTRADA") ||
                                extractedContent.trim().equalsIgnoreCase("INFORMAÇÃO NÃO ENCONTRADA."));

                if (isEssentiallyEmpty || isStrictlyNotFound) {
                    log.warn("IA não encontrou a informação nas evidências.");
                    finalAnswer = new AuditedAnswer(
                            new Answer("A informação solicitada não foi encontrada no documento.",
                                    "A informação solicitada não foi encontrada no documento."),
                            Collections.emptyList());
                } else {
                    log.info("✅ Etapa 2: Construindo resposta auditada.");
                    List<EvidenceRef> evidenceRefs = IntStream.range(0, evidences.size())
                            .mapToObj(i -> new EvidenceRef(i + 1, evidences.get(i).excerpt()))
                            .collect(Collectors.toList());

                    finalAnswer = new AuditedAnswer(
                            new Answer(extractedContent, extractedContent),
                            evidenceRefs);
                }

                String finalJson = objectMapper.writeValueAsString(finalAnswer);
                AIProviderResponse finalResponse = new AIProviderResponse(finalJson, null, "RAG - 2 Etapas", null,
                        InteractionType.RAG_ANALYSIS);
                finalResponse.setEvidences(evidences);
                return finalResponse;

            } catch (Exception e) {
                log.error("Erro ao processar documento com RAG: {}", e.getMessage(), e);
                return new AIProviderResponse(getFailureJson(), null, "RAG Error", null, InteractionType.RAG_ANALYSIS);
            }
        });
    }

    public String expandQueryWithDynamicTerms(String query, DocumentAnalyzer.DocumentProfile profile) {
        if (profile == null || profile.dynamicGlossary.isEmpty()) {
            return query;
        }

        String expanded = query;
        for (Map.Entry<String, String> entry : profile.dynamicGlossary.entrySet()) {
            String acronym = entry.getKey();
            String definition = entry.getValue();

            if (query.contains(acronym) && !query.toLowerCase().contains(definition.toLowerCase())) {
                expanded = expanded.replaceFirst(
                        "\\b" + java.util.regex.Pattern.quote(acronym) + "\\b",
                        acronym + " (" + definition + ")");
            }
        }
        return expanded;
    }

    // --- Private Helpers ---

    private String enrichSystemPromptWithGlossary(String baseSystemPrompt, String userQuery,
            DocumentAnalyzer.DocumentProfile profile) {
        Map<String, String> staticDefinitions = documentAnalyzer.getTermDefinitions();
        StringBuilder glossary = new StringBuilder();
        String lowerQuery = userQuery.toLowerCase();

        for (Map.Entry<String, String> entry : staticDefinitions.entrySet()) {
            if (lowerQuery.contains(entry.getKey())) {
                glossary.append("- ").append(entry.getValue()).append("\n");
            }
        }

        if (profile != null && !profile.dynamicGlossary.isEmpty()) {
            for (Map.Entry<String, String> entry : profile.dynamicGlossary.entrySet()) {
                if (userQuery.contains(entry.getKey())) {
                    glossary.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
            }
        }

        if (glossary.length() > 0) {
            return baseSystemPrompt + "\n\n### GLOSSÁRIO DE DOMÍNIO E REGRAS DE NEGÓCIO:\n" + glossary.toString();
        }

        return baseSystemPrompt;
    }

    private String formatEvidences(List<Evidence> evidences) {
        StringBuilder sb = new StringBuilder();
        int currentLength = 0;
        List<Evidence> selectedEvidences = new ArrayList<>();

        List<Evidence> priorityEvidences = evidences.stream()
                .filter(this::isTableOrMetrics)
                .toList();

        List<Evidence> standardEvidences = evidences.stream()
                .filter(e -> !isTableOrMetrics(e))
                .toList();

        for (Evidence e : priorityEvidences) {
            String formatted = formatEvidenceItem(e, -1);
            if (currentLength + formatted.length() <= MAX_EVIDENCE_CHARS) {
                selectedEvidences.add(e);
                currentLength += formatted.length();
            }
        }

        for (Evidence e : standardEvidences) {
            String formatted = formatEvidenceItem(e, -1);
            if (currentLength + formatted.length() <= MAX_EVIDENCE_CHARS) {
                selectedEvidences.add(e);
                currentLength += formatted.length();
            }
        }

        List<Evidence> finalSelection = evidences.stream()
                .filter(selectedEvidences::contains)
                .toList();

        for (int i = 0; i < finalSelection.size(); i++) {
            sb.append(formatEvidenceItem(finalSelection.get(i), i + 1)).append("\n");
        }

        return sb.toString();
    }

    private boolean isTableOrMetrics(Evidence e) {
        String type = e.contentType() != null ? e.contentType().toLowerCase() : "";
        String excerpt = e.excerpt().toLowerCase();
        return type.contains("table") || type.contains("metric") || excerpt.contains("table_type=")
                || excerpt.contains("has_table=true");
    }

    private String formatEvidenceItem(Evidence e, int index) {
        String cleanedExcerpt = cleanText(e.excerpt().replaceAll("\"", "'"));
        String indexStr = index > 0 ? String.valueOf(index) : "#";
        return String.format(
                """
                        EVIDENCE_%s:
                        - doc: %s
                        - section: %s
                        - page: %d
                        - excerpt: "%s"
                        - score: %.2f
                        """,
                indexStr,
                e.documentId(),
                e.section() != null ? e.section() : "N/A",
                e.page() != null ? e.page() : 0,
                cleanedExcerpt,
                e.score());
    }

    private String cleanText(String text) {
        if (text == null)
            return "";
        return text.replaceAll("\\s+", " ").trim();
    }

    private String getNotFoundJson() {
        return "{\"answer\":{\"markdown\":\"A informação solicitada não foi encontrada no documento.\",\"plainText\":\"A informação solicitada não foi encontrada no documento.\"},\"references\":[]}";
    }

    private String getFailureJson() {
        return "{\"answer\":{\"markdown\":\"❌ Erro interno ao processar a resposta da IA.\",\"plainText\":\"❌ Erro interno ao processar a resposta da IA.\"},\"references\":[]}";
    }
}
