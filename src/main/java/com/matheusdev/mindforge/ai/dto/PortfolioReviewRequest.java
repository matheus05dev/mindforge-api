package com.matheusdev.mindforge.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class PortfolioReviewRequest {
    @NotBlank(message = "A URL do repositório do GitHub é obrigatória.")
    @URL(message = "A URL fornecida é inválida.")
    private String githubRepoUrl;
}
