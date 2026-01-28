package com.matheusdev.mindforge.ai.service;

import com.matheusdev.mindforge.ai.chat.model.ChatMessage;
import com.matheusdev.mindforge.ai.chat.model.ChatSession;
import com.matheusdev.mindforge.ai.dto.ChatRequest;
import com.matheusdev.mindforge.ai.dto.PortfolioReviewRequest;
import com.matheusdev.mindforge.ai.dto.PromptPair;
import com.matheusdev.mindforge.ai.memory.model.UserProfileAI;
import com.matheusdev.mindforge.ai.memory.service.MemoryService;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.exception.BusinessException;
import com.matheusdev.mindforge.integration.github.GitHubClient;
import com.matheusdev.mindforge.integration.model.UserIntegration;
import com.matheusdev.mindforge.integration.repository.UserIntegrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final UserIntegrationRepository userIntegrationRepository;
    private final GitHubClient gitHubClient;
    private final PromptBuilderService promptBuilderService;
    private final ChatService chatService;
    private final AIOrchestrationService aiOrchestrationService;
    private final MemoryService memoryService;

    @Transactional
    public ChatMessage reviewPortfolio(PortfolioReviewRequest request) throws IOException {
        final Long userId = com.matheusdev.mindforge.core.auth.util.SecurityUtils.getCurrentUserId();
        String[] urlParts = request.getGithubRepoUrl().replace("https://github.com/", "").split("/");
        String owner = urlParts[0];
        String repoName = urlParts[1];

        userIntegrationRepository.findByUserIdAndProvider(userId, UserIntegration.Provider.GITHUB)
                .orElseThrow(
                        () -> new BusinessException("O usuário não conectou a conta do GitHub para esta operação."));

        String readmeContent = gitHubClient.getFileContent(userId, owner, repoName, "README.md");
        UserProfileAI userProfile = memoryService.getProfile(userId);

        PromptPair prompts = promptBuilderService.buildPortfolioReviewerPrompt(readmeContent, userProfile);
        ChatSession session = chatService.getOrCreateGenericChatSession(null);
        ChatMessage userMessage = chatService.saveMessage(session, "user", prompts.userPrompt());

        try {
            ChatRequest chatRequest = new ChatRequest(null, null, prompts.userPrompt(), request.getProvider(), null,
                    prompts.systemPrompt());
            AIProviderResponse aiResponse = aiOrchestrationService.handleChatInteraction(chatRequest).get();

            if (aiResponse.getError() != null) {
                throw new BusinessException("Erro no serviço de IA: " + aiResponse.getError());
            }
            return chatService.saveMessage(session, "assistant", aiResponse.getContent());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Falha ao processar a revisão de portfólio com IA.", e);
        }
    }
}
