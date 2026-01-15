package com.matheusdev.mindforge.ai.provider.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AIProviderRequest {
    private String textPrompt;
    private String systemMessage; // Instrução de sistema (Persona/Contexto)
    private String model;         // Modelo específico a ser usado
    private byte[] imageData;
    private String imageMimeType;

    public AIProviderRequest(String textPrompt) {
        this.textPrompt = textPrompt;
    }

    public AIProviderRequest(String textPrompt, String systemMessage, String model) {
        this.textPrompt = textPrompt;
        this.systemMessage = systemMessage;
        this.model = model;
    }

    public AIProviderRequest(String textPrompt, byte[] imageData, String imageMimeType) {
        this.textPrompt = textPrompt;
        this.imageData = imageData;
        this.imageMimeType = imageMimeType;
    }

    public boolean isMultimodal() {
        return imageData != null && imageMimeType != null;
    }
}
