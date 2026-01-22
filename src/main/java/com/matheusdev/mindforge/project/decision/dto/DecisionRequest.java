package com.matheusdev.mindforge.project.decision.dto;

import com.matheusdev.mindforge.project.decision.model.DecisionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DecisionRequest {
    @NotNull(message = "O ID do projeto é obrigatório")
    private Long projectId;

    @NotBlank(message = "O título é obrigatório")
    private String title;

    @NotNull(message = "O status é obrigatório")
    private DecisionStatus status;

    private String context;
    private String decision;
    private String consequences;
    private List<String> tags;
    private String author; // Opcional, pode pegar do contexto de segurança no futuro
}
