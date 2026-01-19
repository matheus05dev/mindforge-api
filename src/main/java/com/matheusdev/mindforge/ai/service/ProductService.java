package com.matheusdev.mindforge.ai.service;

import com.matheusdev.mindforge.ai.chat.model.ChatMessage;
import com.matheusdev.mindforge.ai.chat.model.ChatSession;
import com.matheusdev.mindforge.ai.dto.ChatRequest;
import com.matheusdev.mindforge.ai.dto.ProductThinkerRequest;
import com.matheusdev.mindforge.ai.dto.PromptPair;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final PromptBuilderService promptBuilderService;
    private final ChatService chatService;
    private final AIOrchestrationService aiOrchestrationService;

    @Transactional
    public ChatMessage thinkAsProductManager(ProductThinkerRequest request) {
        PromptPair prompts = promptBuilderService.buildProductThinkerPrompt(request.getFeatureDescription());
        ChatSession session = chatService.getOrCreateGenericChatSession(null);
        ChatMessage userMessage = chatService.saveMessage(session, "user", prompts.userPrompt());

        try {
            ChatRequest chatRequest = new ChatRequest(null, null, prompts.userPrompt(), request.getProvider(), null, prompts.systemPrompt());
            AIProviderResponse aiResponse = aiOrchestrationService.handleChatInteraction(chatRequest).get();

            if (aiResponse.getError() != null) {
                throw new BusinessException("Erro no serviço de IA: " + aiResponse.getError());
            }
            return chatService.saveMessage(session, "assistant", aiResponse.getContent());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Falha ao processar o pensamento de produto com IA.", e);
        }
    }
}
