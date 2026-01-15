package com.matheusdev.mindforge.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GenericAnalysisRequest {
    @NotBlank(message = "A pergunta ou problema é obrigatório.")
    private String question;

    private Long subjectId; // Contexto do estudo (ex: Matemática, Física)
    private Long projectId; // Contexto do projeto
}
