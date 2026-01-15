package com.matheusdev.mindforge.workspace.dto;

import com.matheusdev.mindforge.workspace.model.WorkspaceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WorkspaceRequest {
    @NotBlank
    private String name;
    private String description;
    @NotNull
    private WorkspaceType type;
}
