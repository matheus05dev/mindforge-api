package com.matheusdev.mindforge.workspace.dto;

import com.matheusdev.mindforge.workspace.model.WorkspaceType;
import lombok.Data;

@Data
public class WorkspaceResponse {
    private Long id;
    private String name;
    private String description;
    private WorkspaceType type;
}
