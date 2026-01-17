package com.matheusdev.mindforge.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductThinkerRequest {

    @NotBlank(message = "A descrição da funcionalidade não pode estar em branco.")
    @Schema(description = "Descrição da nova funcionalidade ou ideia a ser analisada.", required = true, example = "Um sistema de gamificação para incentivar os usuários a completarem seus estudos.")
    private String featureDescription;

    @Schema(description = "Provedor de IA a ser usado (ex: 'geminiProvider', 'groqProvider'). Padrão: 'geminiProvider'.")
    private String provider;
}
