package com.matheusdev.mindforge.ai.service;

import com.matheusdev.mindforge.ai.chat.model.ChatMessage;
import com.matheusdev.mindforge.ai.chat.model.ChatSession;
import com.matheusdev.mindforge.ai.dto.PortfolioReviewRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.exception.BusinessException;
import com.matheusdev.mindforge.integration.github.GitHubClient;
import com.matheusdev.mindforge.integration.model.UserIntegration;
import com.matheusdev.mindforge.integration.repository.UserIntegrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final UserIntegrationRepository userIntegrationRepository;
    private final GitHubClient gitHubClient;
    private final PromptBuilderService promptBuilderService;
    private final ChatService chatService;
    private final PromptCacheService promptCacheService;

    @Transactional
    public ChatMessage reviewPortfolio(PortfolioReviewRequest request) throws IOException {
        final Long userId = 1L; // Provisório
        String[] urlParts = request.getGithubRepoUrl().replace("https://github.com/", "").split("/");
        String owner = urlParts[0];
        String repoName = urlParts[1];

        userIntegrationRepository.findByUserIdAndProvider(userId, UserIntegration.Provider.GITHUB)
                .orElseThrow(() -> new BusinessException("O usuário não conectou a conta do GitHub para esta operação."));

        String readmeContent = gitHubClient.getFileContent(userId, owner, repoName, "README.md");

        String prompt = promptBuilderService.buildPortfolioReviewerPrompt(readmeContent);
        ChatSession session = chatService.getOrCreateGenericChatSession(null);
        ChatMessage userMessage = chatService.saveMessage(session, "user", prompt);

        AIProviderResponse aiResponse = promptCacheService.executeRequest(new AIProviderRequest(prompt));

        if (aiResponse.getError() != null) {
            throw new BusinessException("Erro no serviço de IA: " + aiResponse.getError());
        }
        return chatService.saveMessage(session, "assistant", aiResponse.getContent());
    }
}
