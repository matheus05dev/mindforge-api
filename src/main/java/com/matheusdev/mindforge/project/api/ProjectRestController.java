package com.matheusdev.mindforge.project.api;

import com.matheusdev.mindforge.project.dto.LinkRepositoryRequest;
import com.matheusdev.mindforge.project.dto.ProjectRequest;
import com.matheusdev.mindforge.project.dto.ProjectResponse;
import com.matheusdev.mindforge.project.milestone.dto.MilestoneRequest;
import com.matheusdev.mindforge.project.milestone.dto.MilestoneResponse;
import com.matheusdev.mindforge.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Project management")
public class ProjectRestController {

    private final ProjectService service;

    @Operation(summary = "Get all projects", description = "Returns a list of all projects")
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        return ResponseEntity.ok(service.getAllProjects());
    }

    @Operation(summary = "Get a project by ID", description = "Returns a single project")
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long projectId) {
        return ResponseEntity.ok(service.getProjectById(projectId));
    }

    @Operation(summary = "Create a new project", description = "Creates a new project")
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@RequestBody @Valid ProjectRequest request) {
        return ResponseEntity.ok(service.createProject(request));
    }

    @Operation(summary = "Update a project", description = "Updates an existing project")
    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(@PathVariable Long projectId, @RequestBody @Valid ProjectRequest request) {
        return ResponseEntity.ok(service.updateProject(projectId, request));
    }

    @Operation(summary = "Delete a project", description = "Deletes a project")
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long projectId) {
        service.deleteProject(projectId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Link a GitHub repository to a project")
    @PostMapping("/{projectId}/link")
    public ResponseEntity<ProjectResponse> linkRepository(
            @PathVariable Long projectId,
            @RequestBody @Valid LinkRepositoryRequest request) {
        return ResponseEntity.ok(service.linkRepository(projectId, request.getRepoUrl()));
    }

    @Operation(summary = "Add a milestone to a project")
    @PostMapping("/{projectId}/milestones")
    public ResponseEntity<MilestoneResponse> addMilestone(
            @PathVariable Long projectId,
            @RequestBody @Valid MilestoneRequest request) {
        return ResponseEntity.ok(service.addMilestone(projectId, request));
    }

    @Operation(summary = "Update a milestone")
    @PutMapping("/milestones/{milestoneId}")
    public ResponseEntity<MilestoneResponse> updateMilestone(
            @PathVariable Long milestoneId,
            @RequestBody @Valid MilestoneRequest request) {
        return ResponseEntity.ok(service.updateMilestone(milestoneId, request));
    }

    @Operation(summary = "Delete a milestone")
    @DeleteMapping("/milestones/{milestoneId}")
    public ResponseEntity<Void> deleteMilestone(@PathVariable Long milestoneId) {
        service.deleteMilestone(milestoneId);
        return ResponseEntity.noContent().build();
    }
}
