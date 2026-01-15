package com.matheusdev.mindforge.kanban.dto;

import com.matheusdev.mindforge.document.dto.DocumentResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Response for a Kanban task")
public class KanbanTaskResponse {
    @Schema(description = "The ID of the task", example = "1")
    private Long id;
    @Schema(description = "The title of the task", example = "Implement feature X")
    private String title;
    @Schema(description = "The description of the task", example = "Implement the new feature X for the project Y")
    private String description;
    @Schema(description = "The position of the task in the column", example = "0")
    private Integer position;
    @Schema(description = "The ID of the column the task belongs to", example = "1")
    private Long columnId;
    @Schema(description = "The ID of the subject related to the task", example = "1")
    private Long subjectId;
    @Schema(description = "The name of the subject related to the task", example = "Java")
    private String subjectName;
    @Schema(description = "The ID of the project related to the task", example = "1")
    private Long projectId;
    @Schema(description = "The name of the project related to the task", example = "Mind Forge")
    private String projectName;
    @Schema(description = "The documents attached to the task")
    private List<DocumentResponse> documents;
}
