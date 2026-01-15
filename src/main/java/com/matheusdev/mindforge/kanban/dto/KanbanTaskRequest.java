package com.matheusdev.mindforge.kanban.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request to create or update a Kanban task")
public class KanbanTaskRequest {
    @Schema(description = "The title of the task", example = "Implement feature X")
    private String title;
    @Schema(description = "The description of the task", example = "Implement the new feature X for the project Y")
    private String description;
    @Schema(description = "The position of the task in the column", example = "0")
    private Integer position;
    @Schema(description = "The ID of the subject related to the task", example = "1")
    private Long subjectId;
    @Schema(description = "The ID of the project related to the task", example = "1")
    private Long projectId;
}
