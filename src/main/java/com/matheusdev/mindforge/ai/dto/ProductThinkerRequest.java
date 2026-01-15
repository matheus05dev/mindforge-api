package com.matheusdev.mindforge.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductThinkerRequest {
    @NotBlank(message = "A descrição da funcionalidade é obrigatória.")
    private String featureDescription;
}
