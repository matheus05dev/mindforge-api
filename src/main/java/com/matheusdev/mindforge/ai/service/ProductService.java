package com.matheusdev.mindforge.ai.service;

import com.matheusdev.mindforge.ai.chat.model.ChatMessage;
import com.matheusdev.mindforge.ai.chat.model.ChatSession;
import com.matheusdev.mindforge.ai.dto.ProductThinkerRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final PromptBuilderService promptBuilderService;
    private final ChatService chatService;
    private final PromptCacheService promptCacheService;

    @Transactional
    public ChatMessage thinkAsProductManager(ProductThinkerRequest request) {
        String prompt = promptBuilderService.buildProductThinkerPrompt(request.getFeatureDescription());
        ChatSession session = chatService.getOrCreateGenericChatSession(null);
        ChatMessage userMessage = chatService.saveMessage(session, "user", prompt);

        AIProviderResponse aiResponse = promptCacheService.executeRequest(new AIProviderRequest(prompt));

        if (aiResponse.getError() != null) {
            throw new BusinessException("Erro no serviço de IA: " + aiResponse.getError());
        }
        return chatService.saveMessage(session, "assistant", aiResponse.getContent());
    }
}
