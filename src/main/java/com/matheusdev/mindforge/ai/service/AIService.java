package com.matheusdev.mindforge.ai.service;

import com.matheusdev.mindforge.ai.chat.model.ChatMessage;
import com.matheusdev.mindforge.ai.chat.model.ChatSession;
import com.matheusdev.mindforge.ai.dto.*;
import com.matheusdev.mindforge.ai.memory.model.UserProfileAI;
import com.matheusdev.mindforge.ai.memory.service.MemoryService;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.document.model.Document;
import com.matheusdev.mindforge.document.service.DocumentService;
import com.matheusdev.mindforge.document.util.CustomMultipartFile;
import com.matheusdev.mindforge.exception.BusinessException;
import com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeItemResponse;
import com.matheusdev.mindforge.project.model.Project;
import com.matheusdev.mindforge.project.repository.ProjectRepository;
import com.matheusdev.mindforge.study.subject.model.Subject;
import com.matheusdev.mindforge.study.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class AIService {

    private final MemoryService memoryService;
    private final SubjectRepository subjectRepository;
    private final ProjectRepository projectRepository;
    private final PromptBuilderService promptBuilderService;
    private final ChatService chatService;
    private final CodeAnalysisService codeAnalysisService;
    private final KnowledgeItemAIService knowledgeItemAIService;
    private final PortfolioService portfolioService;
    private final ProductService productService;
    private final AIOrchestrationService aiOrchestrationService;
    private final DocumentService documentService;

    @Transactional
    public ChatMessage analyzeCodeForProficiency(CodeAnalysisRequest request) throws IOException {
        return codeAnalysisService.analyzeCodeForProficiency(request);
    }

    @Transactional
    public ChatMessage analyzeGitHubFile(GitHubFileAnalysisRequest request) throws IOException {
        return codeAnalysisService.analyzeGitHubFile(request);
    }

    @Transactional
    public KnowledgeItemResponse modifyKnowledgeItemContent(Long itemId, String instruction, String provider) {
        // Este método agora usa o prompt genérico, mas poderia ter um mais específico.
        PromptPair prompts = promptBuilderService.buildContentModificationPrompt(instruction, instruction);
        // A lógica de modificação do item de conhecimento precisa ser movida para cá ou
        // para um serviço dedicado.
        // Por enquanto, esta chamada está incompleta.
        return knowledgeItemAIService.modifyKnowledgeItemContent(itemId, instruction, provider);
    }

    @Transactional
    public KnowledgeItemResponse transcribeImageAndAppendToKnowledgeItem(Long documentId, Long itemId, String provider)
            throws IOException {
        return knowledgeItemAIService.transcribeImageAndAppendToKnowledgeItem(documentId, itemId, provider);
    }

    @Transactional
    public ChatMessage analyzeGeneric(GenericAnalysisRequest request, String provider) {
        final Long userId = com.matheusdev.mindforge.core.auth.util.SecurityUtils.getCurrentUserId();
        UserProfileAI userProfile = memoryService.getProfile(userId);

        Optional<Subject> subject = request.getSubjectId() != null ? subjectRepository.findById(request.getSubjectId())
                : Optional.empty();
        Optional<Project> project = request.getProjectId() != null ? projectRepository.findById(request.getProjectId())
                : Optional.empty();

        PromptPair prompts = promptBuilderService.buildGenericPrompt(request.getQuestion(), userProfile, subject,
                project);

        ChatSession session = chatService.getOrCreateGenericChatSession(request);
        ChatMessage userMessage = chatService.saveMessage(session, "user", prompts.userPrompt());

        try {
            AIProviderRequest providerRequest = new AIProviderRequest(prompts.userPrompt(), prompts.systemPrompt(),
                    null, provider);
            AIProviderResponse aiResponse = aiOrchestrationService
                    .handleChatInteraction(providerRequest.toChatRequest(provider)).get();

            if (aiResponse.getError() != null) {
                throw new BusinessException("Erro no serviço de IA: " + aiResponse.getError());
            }
            ChatMessage assistantMessage = chatService.saveMessage(session, "assistant", aiResponse.getContent());

            List<Map<String, String>> chatHistory = new ArrayList<>();
            chatHistory.add(Map.of("role", "user", "content", userMessage.getContent()));
            chatHistory.add(Map.of("role", "assistant", "content", assistantMessage.getContent()));
            memoryService.updateUserProfile(userId, chatHistory);

            return assistantMessage;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Erro ao processar a análise genérica com o serviço de IA.", e);
        }
    }

    @Transactional
    public ChatMessage reviewPortfolio(PortfolioReviewRequest request) throws IOException {
        return portfolioService.reviewPortfolio(request);
    }

    @Transactional
    public ChatMessage thinkAsProductManager(ProductThinkerRequest request) {
        return productService.thinkAsProductManager(request);
    }

    @Transactional
    public ChatMessage summarize(SummarizationRequest request) {
        try {
            String summary = aiOrchestrationService.summarizeContent(request.getText()).get();
            ChatMessage response = new ChatMessage();
            response.setRole("assistant");
            response.setContent(summary);
            return response;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Erro ao gerar resumo com IA.", e);
        }
    }
}
