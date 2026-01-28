package com.matheusdev.mindforge.ai.service;

import com.matheusdev.mindforge.ai.chat.model.ChatMessage;
import com.matheusdev.mindforge.ai.chat.model.ChatSession;
import com.matheusdev.mindforge.ai.dto.CodeAnalysisRequest;
import com.matheusdev.mindforge.ai.dto.GitHubFileAnalysisRequest;
import com.matheusdev.mindforge.ai.dto.PromptPair;
import com.matheusdev.mindforge.ai.memory.model.UserProfileAI;
import com.matheusdev.mindforge.ai.memory.service.MemoryService;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.document.model.Document;
import com.matheusdev.mindforge.document.repository.DocumentRepository;
import com.matheusdev.mindforge.document.service.FileStorageService;
import com.matheusdev.mindforge.exception.BusinessException;
import com.matheusdev.mindforge.exception.ResourceNotFoundException;
import com.matheusdev.mindforge.integration.github.GitHubClient;
import com.matheusdev.mindforge.integration.model.UserIntegration;
import com.matheusdev.mindforge.integration.repository.UserIntegrationRepository;
import com.matheusdev.mindforge.project.model.Project;
import com.matheusdev.mindforge.project.repository.ProjectRepository;
import com.matheusdev.mindforge.study.subject.model.Subject;
import com.matheusdev.mindforge.study.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class CodeAnalysisService {

    private final SubjectRepository subjectRepository;
    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final MemoryService memoryService;
    private final PromptBuilderService promptBuilderService;
    private final ChatService chatService;
    private final ProjectRepository projectRepository;
    private final UserIntegrationRepository userIntegrationRepository;
    private final GitHubClient gitHubClient;
    private final AIOrchestrationService aiOrchestrationService;

    @Transactional
    public ChatMessage analyzeCodeForProficiency(CodeAnalysisRequest request) throws IOException {
        final Long userId = com.matheusdev.mindforge.core.auth.util.SecurityUtils.getCurrentUserId();
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assunto de estudo não encontrado com o id: " + request.getSubjectId()));
        String code = getCodeFromRequest(request);
        UserProfileAI userProfile = memoryService.getProfile(userId);

        PromptPair prompts;
        switch (request.getMode()) {
            case ANALYST:
                prompts = promptBuilderService.buildAnalystPrompt(code);
                break;
            case DEBUG_ASSISTANT:
                prompts = promptBuilderService.buildDebugAssistantPrompt(code);
                break;
            case SOCRATIC_TUTOR:
                prompts = promptBuilderService.buildSocraticTutorPrompt(code);
                break;
            case MENTOR:
            default:
                prompts = promptBuilderService.buildMentorPrompt(code, userProfile, subject);
                break;
        }

        ChatSession session = chatService.getOrCreateChatSession(subject, request.getMode().name());
        // Salva o prompt do usuário, que é mais significativo para o histórico
        ChatMessage userMessage = chatService.saveMessage(session, "user", prompts.userPrompt());

        try {
            // Tenta usar GROQ primeiro pela velocidade
            String primaryProvider = "groqProvider";
            String fallbackProvider = "ollamaProvider";

            AIProviderResponse aiResponse;
            try {
                AIProviderRequest providerRequest = new AIProviderRequest(prompts.userPrompt(), prompts.systemPrompt(),
                        null, primaryProvider);
                aiResponse = aiOrchestrationService
                        .handleChatInteraction(providerRequest.toChatRequest(primaryProvider)).get();
                if (aiResponse.getError() != null) {
                    throw new BusinessException("Erro no provedor primário: " + aiResponse.getError());
                }
            } catch (Exception e) {
                // Fallback para Ollama
                AIProviderRequest providerRequest = new AIProviderRequest(prompts.userPrompt(), prompts.systemPrompt(),
                        null, fallbackProvider);
                aiResponse = aiOrchestrationService
                        .handleChatInteraction(providerRequest.toChatRequest(fallbackProvider)).get();
            }

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
            throw new BusinessException("Erro ao processar a análise de código com o serviço de IA.", e);
        }
    }

    @Transactional
    public ChatMessage analyzeGitHubFile(GitHubFileAnalysisRequest request) throws IOException {
        final Long userId = com.matheusdev.mindforge.core.auth.util.SecurityUtils.getCurrentUserId();

        String repoUrl;
        Subject contextSubject;

        // Support both projectId and subjectId
        if (request.getProjectId() != null) {
            Project project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Projeto não encontrado com o id: " + request.getProjectId()));
            repoUrl = project.getGithubRepoUrl();

            // Use first subject as context (or could be improved to use project-related
            // subject)
            contextSubject = subjectRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Nenhum assunto de estudo encontrado para contextualizar a análise."));
        } else if (request.getSubjectId() != null) {
            Subject subject = subjectRepository.findById(request.getSubjectId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Matéria não encontrada com o id: " + request.getSubjectId()));
            repoUrl = subject.getGithubRepoUrl();
            contextSubject = subject;
        } else {
            throw new BusinessException("É necessário fornecer projectId ou subjectId.");
        }

        userIntegrationRepository.findByUserIdAndProvider(userId, UserIntegration.Provider.GITHUB)
                .orElseThrow(() -> new BusinessException("O usuário não conectou a conta do GitHub."));

        if (repoUrl == null || repoUrl.isEmpty()) {
            throw new BusinessException("O projeto/matéria não está vinculado a um repositório do GitHub.");
        }
        String[] urlParts = repoUrl.replace("https://github.com/", "").split("/");
        String owner = urlParts[0];
        String repoName = urlParts[1].replace(".git", "");

        String fileContent = gitHubClient.getFileContent(userId, owner, repoName, request.getFilePath());

        CodeAnalysisRequest internalRequest = new CodeAnalysisRequest();
        internalRequest.setSubjectId(contextSubject.getId());
        internalRequest.setCodeToAnalyze(fileContent);
        internalRequest.setMode(request.getMode());
        return analyzeCodeForProficiency(internalRequest);
    }

    private String getCodeFromRequest(CodeAnalysisRequest request) throws IOException {
        if (request.getCodeToAnalyze() != null && !request.getCodeToAnalyze().isEmpty()) {
            return request.getCodeToAnalyze();
        }
        if (request.getDocumentId() != null) {
            Document doc = documentRepository.findById(request.getDocumentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Documento não encontrado com o id: " + request.getDocumentId()));
            Resource resource = fileStorageService.loadFileAsResource(doc.getFileName());
            return new String(Files.readAllBytes(resource.getFile().toPath()));
        }
        throw new BusinessException("Nenhum código ou ID de documento foi fornecido para análise.");
    }
}
