package com.matheusdev.mindforge.ai.provider.dto;

public record AIProviderRequest(
        String textPrompt,
        String systemMessage,
        String model,
        String preferredProvider,
        boolean multimodal,
        byte[] imageData,
        String imageMimeType
) {
    // Construtor para requisições de texto simples
    public AIProviderRequest(String textPrompt) {
        this(textPrompt, null, null, null, false, null, null);
    }

    // Construtor para requisições de texto com mais contexto
    public AIProviderRequest(String textPrompt, String systemMessage, String model, String preferredProvider) {
        this(textPrompt, systemMessage, model, preferredProvider, false, null, null);
    }
    
    // Construtor para requisições de texto com mais contexto (sem provedor)
    public AIProviderRequest(String textPrompt, String systemMessage, String model) {
        this(textPrompt, systemMessage, model, null, false, null, null);
    }

    // Construtor para requisições multimodais (imagem + texto)
    public AIProviderRequest(String textPrompt, byte[] imageData, String imageMimeType) {
        this(textPrompt, null, null, null, true, imageData, imageMimeType);
    }
}
