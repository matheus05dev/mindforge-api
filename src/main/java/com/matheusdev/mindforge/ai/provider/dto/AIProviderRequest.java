package com.matheusdev.mindforge.ai.provider.dto;

import com.matheusdev.mindforge.ai.dto.ChatRequest;
import lombok.Builder;
import lombok.With;

@Builder
@With
public record AIProviderRequest(
        String textPrompt,
        String systemMessage,
        String model,
        String preferredProvider,
        boolean multimodal,
        byte[] imageData,
        String imageMimeType,
        byte[] documentData,
        String documentMimeType,
        Double temperature,
        Integer maxTokens) {

    // Construtor para requisições de texto simples
    public AIProviderRequest(String textPrompt) {
        this(textPrompt, null, null, null, false, null, null, null, null, null, null);
    }

    // Construtor para requisições de texto com mais contexto
    public AIProviderRequest(String textPrompt, String systemMessage, String model, String preferredProvider) {
        this(textPrompt, systemMessage, model, preferredProvider, false, null, null, null, null, null, null);
    }

    // Construtor para requisições de texto com mais contexto (sem provedor)
    public AIProviderRequest(String textPrompt, String systemMessage, String model) {
        this(textPrompt, systemMessage, model, null, false, null, null, null, null, null, null);
    }

    // Construtor para requisições multimodais (imagem + texto)
    public AIProviderRequest(String textPrompt, byte[] imageData, String imageMimeType) {
        this(textPrompt, null, null, null, true, imageData, imageMimeType, null, null, null, null);
    }

    // Construtor para requisições com documento
    public AIProviderRequest(String textPrompt, byte[] documentData, String documentMimeType, boolean isDocument) {
        this(textPrompt, null, null, null, isDocument, null, null, documentData, documentMimeType, null, null);
    }

    /**
     * Converte este AIProviderRequest em um ChatRequest.
     * 
     * @param provider O nome do provedor a ser usado.
     * @return Um novo ChatRequest.
     */
    public ChatRequest toChatRequest(String provider) {
        return new ChatRequest(null, null, this.textPrompt, provider, this.model, this.systemMessage);
    }
}
