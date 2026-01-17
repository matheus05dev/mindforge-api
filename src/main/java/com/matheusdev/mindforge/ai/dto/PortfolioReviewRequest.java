package com.matheusdev.mindforge.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PortfolioReviewRequest {

    @NotBlank(message = "A URL do repositório do GitHub não pode estar em branco.")
    @Schema(description = "URL completa do repositório do GitHub a ser analisado.", required = true, example = "https://github.com/matheusdev/mindforge-api")
    private String githubRepoUrl;

    @Schema(description = "Provedor de IA a ser usado (ex: 'geminiProvider', 'groqProvider'). Padrão: 'geminiProvider'.")
    private String provider;
}
