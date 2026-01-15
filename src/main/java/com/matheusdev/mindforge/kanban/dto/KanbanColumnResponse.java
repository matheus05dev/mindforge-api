package com.matheusdev.mindforge.kanban.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "Response for a Kanban column")
public class KanbanColumnResponse {
    @Schema(description = "The ID of the column", example = "1")
    private Long id;
    @Schema(description = "The name of the column", example = "To Do")
    private String name;
    @Schema(description = "The position of the column in the board", example = "0")
    private Integer position;
    @Schema(description = "The tasks in the column")
    private List<KanbanTaskResponse> tasks;
}
