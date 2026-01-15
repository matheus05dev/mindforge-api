package com.matheusdev.mindforge.kanban.api;

import com.matheusdev.mindforge.kanban.dto.KanbanColumnRequest;
import com.matheusdev.mindforge.kanban.dto.KanbanColumnResponse;
import com.matheusdev.mindforge.kanban.dto.KanbanTaskRequest;
import com.matheusdev.mindforge.kanban.dto.KanbanTaskResponse;
import com.matheusdev.mindforge.kanban.service.KanbanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kanban")
@RequiredArgsConstructor
@Tag(name = "Kanban", description = "Kanban board management")
public class KanbanRestController {

    private final KanbanService service;

    @Operation(summary = "Get the Kanban board", description = "Returns all columns and tasks in the Kanban board")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the board")
    })
    @GetMapping("/board")
    public ResponseEntity<List<KanbanColumnResponse>> getBoard() {
        return ResponseEntity.ok(service.getBoard());
    }

    @Operation(summary = "Create a new column", description = "Creates a new column in the Kanban board")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully created the column")
    })
    @PostMapping("/columns")
    public ResponseEntity<KanbanColumnResponse> createColumn(@RequestBody KanbanColumnRequest request) {
        return ResponseEntity.ok(service.createColumn(request));
    }

    @Operation(summary = "Create a new task", description = "Creates a new task in a specific column")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully created the task")
    })
    @PostMapping("/columns/{columnId}/tasks")
    public ResponseEntity<KanbanTaskResponse> createTask(@PathVariable Long columnId, @RequestBody KanbanTaskRequest request) {
        return ResponseEntity.ok(service.createTask(columnId, request));
    }

    @Operation(summary = "Move a task", description = "Moves a task to a different column")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully moved the task")
    })
    @PutMapping("/tasks/{taskId}/move/{targetColumnId}")
    public ResponseEntity<KanbanTaskResponse> moveTask(@PathVariable Long taskId, @PathVariable Long targetColumnId) {
        return ResponseEntity.ok(service.moveTask(taskId, targetColumnId));
    }

    @Operation(summary = "Update a task", description = "Updates an existing task")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated the task")
    })
    @PutMapping("/tasks/{taskId}")
    public ResponseEntity<KanbanTaskResponse> updateTask(@PathVariable Long taskId, @RequestBody KanbanTaskRequest request) {
        return ResponseEntity.ok(service.updateTask(taskId, request));
    }

    @Operation(summary = "Delete a task", description = "Deletes a task")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully deleted the task")
    })
    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
        service.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update a column", description = "Updates an existing column")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated the column")
    })
    @PutMapping("/columns/{columnId}")
    public ResponseEntity<KanbanColumnResponse> updateColumn(@PathVariable Long columnId, @RequestBody KanbanColumnRequest request) {
        return ResponseEntity.ok(service.updateColumn(columnId, request));
    }

    @Operation(summary = "Delete a column", description = "Deletes a column")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully deleted the column")
    })
    @DeleteMapping("/columns/{columnId}")
    public ResponseEntity<Void> deleteColumn(@PathVariable Long columnId) {
        service.deleteColumn(columnId);
        return ResponseEntity.noContent().build();
    }
}
