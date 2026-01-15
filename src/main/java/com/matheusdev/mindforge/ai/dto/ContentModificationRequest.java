package com.matheusdev.mindforge.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContentModificationRequest {
    @NotBlank(message = "A instrução é obrigatória.")
    private String instruction; // Ex: "Resuma este texto", "Corrija a gramática", "Traduza para inglês"
}
