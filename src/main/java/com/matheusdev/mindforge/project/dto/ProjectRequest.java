package com.matheusdev.mindforge.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProjectRequest {
    @NotNull(message = "O ID do Workspace é obrigatório.")
    private Long workspaceId;

    @NotBlank(message = "O nome do projeto é obrigatório.")
    private String name;

    private String description;
}
