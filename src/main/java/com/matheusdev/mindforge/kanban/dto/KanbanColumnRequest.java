package com.matheusdev.mindforge.kanban.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request to create or update a Kanban column")
public class KanbanColumnRequest {
    @Schema(description = "The name of the column", example = "To Do")
    private String name;
    @Schema(description = "The position of the column in the board", example = "0")
    private Integer position;
}
