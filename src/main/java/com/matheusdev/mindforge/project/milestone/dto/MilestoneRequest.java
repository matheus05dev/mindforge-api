package com.matheusdev.mindforge.project.milestone.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MilestoneRequest {
    // Não precisamos do workspaceId aqui, pois o milestone é adicionado a um
    // projeto
    // que já existe e, portanto, já está em um workspace.

    // Title is required for creation but optional for updates
    private String title;

    private String description;

    private LocalDate dueDate;

    private boolean completed;
}
