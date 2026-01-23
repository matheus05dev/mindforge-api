package com.matheusdev.mindforge.ai.service.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusdev.mindforge.ai.provider.AIProvider;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.ai.service.PromptCacheService;
import com.matheusdev.mindforge.ai.service.WebSearchService;
import com.matheusdev.mindforge.study.roadmap.dto.GeneratedRoadmapDTO;
import com.matheusdev.mindforge.study.roadmap.dto.RoadmapDTOs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoadmapGeneratorService {

    private final Map<String, AIProvider> aiProviders;
    private final PromptCacheService promptCacheService;
    private final ObjectMapper objectMapper;
    private final WebSearchService webSearchService;

    private final com.fasterxml.jackson.core.type.TypeReference<GeneratedRoadmapDTO> roadmapTypeRef = new com.fasterxml.jackson.core.type.TypeReference<>() {
    };

    private static final String FALLBACK_PROVIDER = "groqProvider";

    private AIProvider getProvider(String providerName) {
        AIProvider provider = aiProviders.get(providerName);
        if (provider == null) {
            throw new IllegalArgumentException("Provedor de IA desconhecido: " + providerName);
        }
        return provider;
    }

    private CompletableFuture<AIProviderResponse> executeAndLogTask(AIProviderRequest request, AIProvider provider,
            String taskName) {
        log.debug("Enviando requisição Roadmap '{}' para o provedor '{}'", taskName,
                provider.getClass().getSimpleName());
        return promptCacheService.executeWithCache(provider, request);
    }

    /**
     * Gera um Roadmap de estudos completo com recursos da web.
     */
    public CompletableFuture<RoadmapDTOs.RoadmapResponse> generateRoadmap(String topic, String duration,
            String difficulty) {
        log.info(">>> [ROADMAP GENERATOR] Gerando Roadmap: Tópico={}, Duração={}, Dificuldade={}", topic, duration,
                difficulty);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Gerar Estrutura do Roadmap
                String systemPrompt = "Você é um mentor especialista em planejamento de carreira e estudos.";
                String jsonStructure = """
                        {
                          "title": "Título do Roadmap",
                          "description": "Descrição inspiradora",
                          "items": [
                            {
                              "title": "Semana 1: Fundamentos",
                              "description": "O que será aprendido...",
                              "searchQuery": "query otimizada para buscar recursos para este tópico específico (ex: 'java basics tutorial site:oracle.com')"
                            }
                          ]
                        }""";

                String userPrompt = String.format(
                        """
                                Crie um plano de estudos detalhado de %s para aprender "%s".
                                Nível: %s.

                                FOCO: Recursos gratuitos de alta qualidade (Cisco NetAcad, Khan Academy, Documentação Oficial, YouTube Edu).

                                Regras:
                                1. Retorne APENAS um JSON válido seguindo estritamente essa estrutura:
                                %s
                                2. O campo 'searchQuery' é CRITICO. Crie uma query de busca específica para encontrar cursos/artigos gratuitos sobre o tema daquela semana/módulo.
                                3. Não inclua markdown.
                                """,
                        duration, topic, difficulty, jsonStructure);

                AIProviderRequest request = AIProviderRequest.builder()
                        .textPrompt(userPrompt)
                        .systemMessage(systemPrompt)
                        .preferredProvider(FALLBACK_PROVIDER)
                        .temperature(0.7)
                        .maxTokens(4096)
                        .build();

                AIProviderResponse initialResponse = executeAndLogTask(request, getProvider(FALLBACK_PROVIDER),
                        "generate-roadmap-structure").get();

                // --- NOTE: Quality Gate removed for brevity in extraction, simplified to
                // direct parsing + cleaning ---
                // Re-add Quality Gate if needed by injecting ResponseValidationService or
                // similar

                String content = cleanJson(initialResponse.getContent());
                GeneratedRoadmapDTO rawRoadmap = objectMapper.readValue(content, roadmapTypeRef);

                // 2. Enriquecer com Recursos Reais (Web Search)
                List<RoadmapDTOs.RoadmapItemResponse> finalItems = new ArrayList<>();
                int index = 1;

                for (GeneratedRoadmapDTO.GeneratedItemDTO item : rawRoadmap.getItems()) {
                    List<RoadmapDTOs.ResourceLink> resources = new ArrayList<>();
                    String descriptionToUse = item.getDescription();

                    if (item.getSearchQuery() != null && !item.getSearchQuery().isBlank()) {
                        try {
                            String enrichedQuery = item.getSearchQuery()
                                    + " (course OR tutorial OR documentation) -paid -udemy";
                            List<String> searchResults = webSearchService.search(enrichedQuery);

                            if (searchResults.isEmpty()) {
                                String broadQuery = item.getSearchQuery() + " tutorial free";
                                searchResults = webSearchService.search(broadQuery);
                            }

                            for (String res : searchResults) {
                                String title = "Recurso Sugerido";
                                String url = "#";
                                String[] lines = res.split("\\n");
                                for (String line : lines) {
                                    if (line.startsWith("Title: "))
                                        title = line.substring(7).trim();
                                    else if (line.startsWith("URL: "))
                                        url = line.substring(5).trim();
                                }
                                if (!url.equals("#") && !url.isBlank()) {
                                    resources.add(new RoadmapDTOs.ResourceLink(title, url, "Link"));
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Falha na busca para item {}: {}", item.getTitle(), e.getMessage());
                        }
                    }

                    // --- LAST RESORT FALLBACK ---
                    if (resources.isEmpty()) {
                        try {
                            String googleQuery = URLEncoder.encode(item.getTitle() + " tutorial",
                                    StandardCharsets.UTF_8);
                            resources.add(new RoadmapDTOs.ResourceLink("Pesquisar no Google (Sugerido)",
                                    "https://www.google.com/search?q=" + googleQuery, "Link"));

                            String guidePrompt = String.format(
                                    "Você é um tutor experiente. O usuário está estudando '%s'. " +
                                            "Não encontramos links diretos. Gere um 'Mini-Guia Rápido' de 3 passos práticos ou conceitos chave para ele estudar este tópico agora. "
                                            +
                                            "Seja direto, prático e breve (max 3 bullets). Sem markdown complexo (apenas texto simples). "
                                            +
                                            "Foco: %s",
                                    item.getTitle(), item.getDescription());

                            AIProviderRequest guideRequest = AIProviderRequest.builder()
                                    .textPrompt(guidePrompt)
                                    .systemMessage("Você é um assistente educacional direto e útil.")
                                    .preferredProvider(FALLBACK_PROVIDER)
                                    .build();

                            AIProvider fallbackProvider = getProvider(FALLBACK_PROVIDER);
                            if (fallbackProvider != null) {
                                AIProviderResponse guideResponse = fallbackProvider.executeTask(guideRequest).get();
                                String guideText = guideResponse.getContent();
                                descriptionToUse = descriptionToUse + "\n\n💡 **Guia Rápido (IA):**\n" + guideText;
                            }

                        } catch (Exception e) {
                            log.error("Falha no Fallback de Emergência para item {}: {}", item.getTitle(),
                                    e.getMessage());
                        }
                    }

                    finalItems.add(RoadmapDTOs.RoadmapItemResponse.builder()
                            .id((long) index)
                            .orderIndex(index++)
                            .title(item.getTitle())
                            .description(descriptionToUse)
                            .resources(resources.stream().limit(3).collect(Collectors.toList()))
                            .build());
                }

                return RoadmapDTOs.RoadmapResponse.builder()
                        .title(rawRoadmap.getTitle())
                        .description(rawRoadmap.getDescription())
                        .targetAudience(difficulty)
                        .items(finalItems)
                        .build();

            } catch (Exception e) {
                log.error("Erro fatal gerando roadmap: {}", e.getMessage(), e);
                throw new RuntimeException("Falha ao gerar roadmap IA", e);
            }
        });
    }

    private String cleanJson(String content) {
        if (content == null)
            return "{}";
        if (content.contains("```json")) {
            content = content.substring(content.indexOf("```json") + 7);
            if (content.contains("```"))
                content = content.substring(0, content.indexOf("```"));
        } else if (content.contains("```")) {
            content = content.substring(content.indexOf("```") + 3);
            if (content.contains("```"))
                content = content.substring(0, content.indexOf("```"));
        }
        return content.trim();
    }
}
