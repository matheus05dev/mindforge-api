package com.matheusdev.mindforge.ai.service;

import com.matheusdev.mindforge.ai.chat.model.ChatMessage;
import com.matheusdev.mindforge.ai.chat.model.ChatSession;
import com.matheusdev.mindforge.ai.dto.*;
import com.matheusdev.mindforge.ai.memory.model.UserProfileAI;
import com.matheusdev.mindforge.ai.memory.service.MemoryService;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.exception.BusinessException;
import com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeItemResponse;
import com.matheusdev.mindforge.project.model.Project;
import com.matheusdev.mindforge.project.repository.ProjectRepository;
import com.matheusdev.mindforge.study.subject.model.Subject;
import com.matheusdev.mindforge.study.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AIService {

    private final PromptCacheService promptCacheService;
    private final MemoryService memoryService;
    private final SubjectRepository subjectRepository;
    private final ProjectRepository projectRepository;
    
    private final PromptBuilderService promptBuilderService;
    private final ChatService chatService;
    private final CodeAnalysisService codeAnalysisService;
    private final KnowledgeItemAIService knowledgeItemAIService;
    private final PortfolioService portfolioService;
    private final ProductService productService;
    private final AIContextService aiContextService;

    @Transactional
    public ChatMessage analyzeCodeForProficiency(CodeAnalysisRequest request) throws IOException {
        return codeAnalysisService.analyzeCodeForProficiency(request);
    }

    @Transactional
    public ChatMessage analyzeGitHubFile(GitHubFileAnalysisRequest request) throws IOException {
        return codeAnalysisService.analyzeGitHubFile(request);
    }

    @Transactional
    public KnowledgeItemResponse modifyKnowledgeItemContent(Long itemId, String instruction) {
        return knowledgeItemAIService.modifyKnowledgeItemContent(itemId, instruction);
    }

    @Transactional
    public KnowledgeItemResponse transcribeImageAndAppendToKnowledgeItem(Long documentId, Long itemId) throws IOException {
        return knowledgeItemAIService.transcribeImageAndAppendToKnowledgeItem(documentId, itemId);
    }

    @Transactional
    public ChatMessage analyzeGeneric(GenericAnalysisRequest request) {
        final Long userId = 1L; // Provisório
        String contextInfo = getContextInfo(request);
        UserProfileAI userProfile = memoryService.getProfile(userId);
        String profileSummary = userProfile.getSummary();
        
        // Constrói System Message e seleciona Modelo
        String systemMessage = aiContextService.buildSystemMessage(userProfile, "Assistente Especialista");
        String model = aiContextService.selectModel(userProfile, false); // Tarefa genérica pode ser menos complexa

        String prompt = promptBuilderService.buildGenericPrompt(request.getQuestion(), contextInfo, profileSummary);
        ChatSession session = chatService.getOrCreateGenericChatSession(request);
        ChatMessage userMessage = chatService.saveMessage(session, "user", prompt);
        
        AIProviderRequest aiRequest = new AIProviderRequest(prompt, systemMessage, model);
        AIProviderResponse aiResponse = promptCacheService.executeRequest(aiRequest);
        
        if (aiResponse.getError() != null) {
            throw new BusinessException("Erro no serviço de IA: " + aiResponse.getError());
        }
        ChatMessage assistantMessage = chatService.saveMessage(session, "assistant", aiResponse.getContent());
        List<Map<String, String>> chatHistory = new ArrayList<>();
        chatHistory.add(Map.of("role", "user", "content", userMessage.getContent()));
        chatHistory.add(Map.of("role", "assistant", "content", assistantMessage.getContent()));
        memoryService.updateUserProfile(userId, chatHistory);
        return assistantMessage;
    }

    @Transactional
    public ChatMessage reviewPortfolio(PortfolioReviewRequest request) throws IOException {
        return portfolioService.reviewPortfolio(request);
    }

    @Transactional
    public ChatMessage thinkAsProductManager(ProductThinkerRequest request) {
        return productService.thinkAsProductManager(request);
    }

    private String getContextInfo(GenericAnalysisRequest request) {
        if (request.getSubjectId() != null) {
            return "no contexto do assunto de estudo: " + subjectRepository.findById(request.getSubjectId()).map(Subject::getName).orElse("Assunto desconhecido");
        }
        if (request.getProjectId() != null) {
            return "no contexto do projeto: " + projectRepository.findById(request.getProjectId()).map(Project::getName).orElse("Projeto desconhecido");
        }
        return "em um contexto geral";
    }
}
