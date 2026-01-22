package com.matheusdev.mindforge.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SummarizationRequest {
    @NotBlank(message = "O texto para resumo é obrigatório.")
    private String text;
}
