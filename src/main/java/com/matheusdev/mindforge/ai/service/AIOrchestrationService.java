package com.matheusdev.mindforge.ai.service;

import com.matheusdev.mindforge.ai.dto.ChatRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.ai.service.generator.QuizGeneratorService;
import com.matheusdev.mindforge.ai.service.generator.RoadmapGeneratorService;
import com.matheusdev.mindforge.ai.service.orchestrator.ChatOrchestrator;
import com.matheusdev.mindforge.ai.service.orchestrator.DocumentAnalysisOrchestrator;
import com.matheusdev.mindforge.ai.service.orchestrator.InternalAnalysisService;
import com.matheusdev.mindforge.ai.service.orchestrator.KnowledgeAssistOrchestrator;
import com.matheusdev.mindforge.ai.service.orchestrator.StudyNoteAssistOrchestrator;
import com.matheusdev.mindforge.study.roadmap.dto.RoadmapDTOs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
/**
 * Serviço central de orquestração de Inteligência Artificial.
 * REFACTORED: Agora atua como um FAÇADE, delegando a responsabilidade para
 * orquestradores especializados.
 * 
 * - ChatOrchestrator: Conversas e fluxo RAG conversacional.
 * - InternalAnalysisService: Tarefas headless e sumarização.
 * - DocumentAnalysisOrchestrator: Análise de arquivos (One-Shot, Map-Reduce,
 * etc).
 * - QuizGeneratorService: Geração de avaliações.
 * - RoadmapGeneratorService: Planos de estudo.
 * - KnowledgeAssistOrchestrator: Assistente de escrita e edição (Notion-like).
 * - StudyNoteAssistOrchestrator: Assistente para notas de estudo.
 */
public class AIOrchestrationService {

    private final ChatOrchestrator chatOrchestrator;
    private final InternalAnalysisService internalAnalysisService;
    private final QuizGeneratorService quizGeneratorService;
    private final RoadmapGeneratorService roadmapGeneratorService;
    private final DocumentAnalysisOrchestrator documentAnalysisOrchestrator;
    private final KnowledgeAssistOrchestrator knowledgeAssistOrchestrator;
    private final StudyNoteAssistOrchestrator studyNoteAssistOrchestrator;

    public CompletableFuture<AIProviderResponse> executeInternalAnalysis(String prompt, String systemMessage) {
        return internalAnalysisService.executeInternalAnalysis(prompt, systemMessage);
    }

    public CompletableFuture<String> summarizeContent(String content) {
        return internalAnalysisService.summarizeContent(content);
    }

    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "aiService", fallbackMethod = "fallbackChatInteraction")
    public CompletableFuture<AIProviderResponse> handleChatInteraction(ChatRequest chatRequest) {
        return chatOrchestrator.handleChatInteraction(chatRequest);
    }

    public CompletableFuture<AIProviderResponse> fallbackChatInteraction(ChatRequest chatRequest, Throwable t) {
        log.error("AI Service Fallback triggered for chat: {}", t.getMessage());
        AIProviderResponse response = new AIProviderResponse();
        response.setContent(
                "O serviço de IA está temporariamente indisponível. Por favor, tente novamente em alguns instantes.");
        response.setError("Service Unavailable: " + t.getMessage());
        return CompletableFuture.completedFuture(response);
    }

    public CompletableFuture<List<com.matheusdev.mindforge.study.quiz.dto.QuizQuestionRequest>> generateQuizQuestions(
            String context, String topic, String difficulty, int count) {
        return quizGeneratorService.generateQuizQuestions(context, topic, difficulty, count);
    }

    public CompletableFuture<RoadmapDTOs.RoadmapResponse> generateRoadmap(String topic, String duration,
            String difficulty) {
        return roadmapGeneratorService.generateRoadmap(topic, duration, difficulty);
    }

    public CompletableFuture<AIProviderResponse> handleFileAnalysis(String userPrompt, String providerName,
            MultipartFile file) throws IOException {
        return documentAnalysisOrchestrator.handleFileAnalysis(userPrompt, providerName, file);
    }

    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "aiService", fallbackMethod = "fallbackKnowledgeAssist")
    public CompletableFuture<com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAIResponse> processKnowledgeAssist(
            com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAIRequest request) {
        return knowledgeAssistOrchestrator.processKnowledgeAssist(request);
    }

    public CompletableFuture<com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAIResponse> fallbackKnowledgeAssist(
            com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAIRequest request, Throwable t) {
        log.error("AI Service Fallback triggered for assist: {}", t.getMessage());
        return CompletableFuture.completedFuture(new com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAIResponse(
                "Serviço indisponível temporariamente.",
                false,
                "Circuit Open"));
    }

    public CompletableFuture<com.matheusdev.mindforge.study.note.dto.StudyNoteAIResponse> processStudyNoteAssist(
            com.matheusdev.mindforge.study.note.dto.StudyNoteAIRequest request) {
        return studyNoteAssistOrchestrator.processStudyNoteAssist(request);
    }

}
