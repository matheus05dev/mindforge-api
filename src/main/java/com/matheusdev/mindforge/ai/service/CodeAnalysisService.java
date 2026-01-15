package com.matheusdev.mindforge.ai.service;

import com.matheusdev.mindforge.ai.chat.model.ChatMessage;
import com.matheusdev.mindforge.ai.chat.model.ChatSession;
import com.matheusdev.mindforge.ai.dto.CodeAnalysisRequest;
import com.matheusdev.mindforge.ai.dto.GitHubFileAnalysisRequest;
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

@Service
@RequiredArgsConstructor
public class CodeAnalysisService {

    private final SubjectRepository subjectRepository;
    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final MemoryService memoryService;
    private final PromptBuilderService promptBuilderService;
    private final ChatService chatService;
    private final PromptCacheService promptCacheService;
    private final ProjectRepository projectRepository;
    private final UserIntegrationRepository userIntegrationRepository;
    private final GitHubClient gitHubClient;
    private final AIContextService aiContextService;

    @Transactional
    public ChatMessage analyzeCodeForProficiency(CodeAnalysisRequest request) throws IOException {
        final Long userId = 1L; // Provisório
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Assunto de estudo não encontrado com o id: " + request.getSubjectId()));
        String code = getCodeFromRequest(request);
        UserProfileAI userProfile = memoryService.getProfile(userId);
        String profileSummary = userProfile.getSummary();

        // Constrói o System Message com base no perfil
        String baseRole = request.getMode().name(); // Ex: MENTOR, ANALYST
        String systemMessage = aiContextService.buildSystemMessage(userProfile, baseRole);
        
        // Seleciona o modelo (Análise de código geralmente é complexa)
        String model = aiContextService.selectModel(userProfile, true);

        String prompt;
        switch (request.getMode()) {
            case ANALYST:
                prompt = promptBuilderService.buildAnalystPrompt(code, subject, profileSummary);
                break;
            case DEBUG_ASSISTANT:
                prompt = promptBuilderService.buildDebugAssistantPrompt(code);
                break;
            case SOCRATIC_TUTOR:
                prompt = promptBuilderService.buildSocraticTutorPrompt(code);
                break;
            case MENTOR:
            default:
                prompt = promptBuilderService.buildMentorPrompt(code, subject, profileSummary);
                break;
        }

        ChatSession session = chatService.getOrCreateChatSession(subject, request.getMode().name());
        ChatMessage userMessage = chatService.saveMessage(session, "user", prompt);

        // Cria a requisição enriquecida com System Message e Modelo
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
    public ChatMessage analyzeGitHubFile(GitHubFileAnalysisRequest request) throws IOException {
        final Long userId = 1L; // Provisório
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado com o id: " + request.getProjectId()));

        userIntegrationRepository.findByUserIdAndProvider(userId, UserIntegration.Provider.GITHUB)
                .orElseThrow(() -> new BusinessException("O usuário não conectou a conta do GitHub."));

        String repoUrl = project.getGithubRepoUrl();
        if (repoUrl == null || repoUrl.isEmpty()) {
            throw new BusinessException("O projeto não está vinculado a um repositório do GitHub.");
        }
        String[] urlParts = repoUrl.replace("https://github.com/", "").split("/");
        String owner = urlParts[0];
        String repoName = urlParts[1];

        String fileContent = gitHubClient.getFileContent(userId, owner, repoName, request.getFilePath());

        Subject subject = subjectRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum assunto de estudo encontrado para contextualizar a análise."));
        CodeAnalysisRequest internalRequest = new CodeAnalysisRequest();
        internalRequest.setSubjectId(subject.getId());
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
                    .orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado com o id: " + request.getDocumentId()));
            Resource resource = fileStorageService.loadFileAsResource(doc.getFileName());
            return new String(Files.readAllBytes(resource.getFile().toPath()));
        }
        throw new BusinessException("Nenhum código ou ID de documento foi fornecido para análise.");
    }
}
