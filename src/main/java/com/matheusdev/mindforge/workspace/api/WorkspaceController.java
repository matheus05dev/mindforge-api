package com.matheusdev.mindforge.workspace.api;

import com.matheusdev.mindforge.workspace.dto.WorkspaceRequest;
import com.matheusdev.mindforge.workspace.dto.WorkspaceResponse;
import com.matheusdev.mindforge.workspace.mapper.WorkspaceMapper;
import com.matheusdev.mindforge.workspace.model.Workspace;
import com.matheusdev.mindforge.workspace.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final WorkspaceMapper workspaceMapper;

    @GetMapping
    public ResponseEntity<List<WorkspaceResponse>> getAllWorkspaces() {
        return ResponseEntity.ok(workspaceService.findAll().stream()
                .map(workspaceMapper::toResponse)
                .collect(Collectors.toList()));
    }

    @PostMapping
    public ResponseEntity<WorkspaceResponse> createWorkspace(@RequestBody @Valid WorkspaceRequest request) {
        Workspace workspace = workspaceMapper.toEntity(request);
        Workspace created = workspaceService.create(workspace);
        return ResponseEntity.ok(workspaceMapper.toResponse(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkspaceResponse> getWorkspaceById(@PathVariable Long id) {
        Workspace workspace = workspaceService.findById(id);
        return ResponseEntity.ok(workspaceMapper.toResponse(workspace));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkspaceResponse> updateWorkspace(
            @PathVariable Long id,
            @RequestBody @Valid WorkspaceRequest request) {
        Workspace workspace = workspaceMapper.toEntity(request);
        Workspace updated = workspaceService.update(id, workspace);
        return ResponseEntity.ok(workspaceMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkspace(@PathVariable Long id) {
        workspaceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
