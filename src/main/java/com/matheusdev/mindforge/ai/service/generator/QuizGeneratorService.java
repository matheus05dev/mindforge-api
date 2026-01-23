package com.matheusdev.mindforge.ai.service.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusdev.mindforge.ai.provider.AIProvider;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.service.PromptCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizGeneratorService {

    private final Map<String, AIProvider> aiProviders;
    private final PromptCacheService promptCacheService;
    private final ObjectMapper objectMapper;

    // JSON Parser
    private final com.fasterxml.jackson.core.type.TypeReference<List<com.matheusdev.mindforge.study.quiz.dto.QuizQuestionRequest>> quizListTypeRef = new com.fasterxml.jackson.core.type.TypeReference<>() {
    };

    private static final String FALLBACK_PROVIDER = "groqProvider";

    private AIProvider getProvider(String providerName) {
        AIProvider provider = aiProviders.get(providerName);
        if (provider == null) {
            throw new IllegalArgumentException("Provedor de IA desconhecido: " + providerName);
        }
        return provider;
    }

    /**
     * Gera uma lista de questões de quiz baseada em um contexto fornecido.
     *
     * @param context    Conteúdo base (notas, web search, etc).
     * @param topic      Tópico específico (opcional).
     * @param difficulty Dificuldade (EASY, MEDIUM, HARD).
     * @param count      Número de questões.
     * @return Lista de questões geradas.
     */
    public CompletableFuture<List<com.matheusdev.mindforge.study.quiz.dto.QuizQuestionRequest>> generateQuizQuestions(
            String context, String topic, String difficulty, int count) {

        log.info(">>> [QUIZ GENERATOR] Gerando Quiz: {} questões, Dif: {}, Tópico: {}", count, difficulty, topic);

        String systemPrompt = "Você é um professor expert criando um exame.";

        String jsonStructure = """
                [
                  {
                    "question": "Texto da pergunta",
                    "options": ["Opção A", "Opção B", "Opção C", "Opção D"],
                    "correctAnswer": 0, // Índice da resposta correta (0-3)
                    "explanation": "Explicação detalhada do porquê está correto"
                  }
                ]""";

        String userPrompt = String.format(
                """
                        Crie um quiz com %d questões de múltipla escolha sobre o tópico: "%s".
                        Nível de Dificuldade: %s.

                        Use o seguinte contexto como base para as perguntas (mas pode trazer conhecimentos gerais se necessário):
                        --- CONTEXTO ---
                        %s
                        --- FIM CONTEXTO ---

                        Regras:
                        1. Retorne APENAS um JSON válido seguindo estritamente essa estrutura:
                        %s
                        2. O campo 'options' deve ser um ARRAY JSON regular (ex: ["A", "B"]).
                        3. Não inclua markdown (```json), apenas o JSON puro.
                        4. Certifique-se de que a resposta correta esteja correta e a explicação seja didática.
                        """,
                count, topic != null ? topic : "Geral", difficulty, context, jsonStructure);

        // Forçar uso do modelo mais inteligente para garantir JSON correto
        String providerName = FALLBACK_PROVIDER;
        AIProvider provider = getProvider(providerName);

        AIProviderRequest request = AIProviderRequest.builder()
                .textPrompt(userPrompt)
                .systemMessage(systemPrompt)
                .preferredProvider(providerName)
                .temperature(0.7)
                .maxTokens(4096)
                .build();

        // REFACTORED: Fully Async Chain using executeWithCache directly
        return promptCacheService.executeWithCache(provider, request)
                .handle((response, throwable) -> {
                    if (throwable != null) {
                        log.error("Erro no Quiz Generator: {}", throwable.getMessage());
                        throw new RuntimeException(throwable);
                    }
                    return response;
                })
                .thenApply(response -> {
                    String content = response.getContent();
                    try {
                        // Robust cleanup: Extract JSON array from [ to ]
                        int startIndex = content.indexOf("[");
                        int endIndex = content.lastIndexOf("]");

                        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                            content = content.substring(startIndex, endIndex + 1);
                        } else {
                            // Fallback for markdown stripping
                            if (content.startsWith("```json"))
                                content = content.substring(7);
                            if (content.startsWith("```"))
                                content = content.substring(3);
                            if (content.endsWith("```"))
                                content = content.substring(0, content.length() - 3);
                        }

                        return objectMapper.readValue(content.trim(), quizListTypeRef);
                    } catch (Exception e) {
                        log.error("Erro ao parsear JSON do quiz", e);
                        // Return empty list on parse error instead of crashing
                        return Collections.<com.matheusdev.mindforge.study.quiz.dto.QuizQuestionRequest>emptyList();
                    }
                });
    }
}
