package com.matheusdev.mindforge.study.api;

import com.matheusdev.mindforge.study.dto.StudySessionRequest;
import com.matheusdev.mindforge.study.dto.StudySessionResponse;
import com.matheusdev.mindforge.study.subject.dto.SubjectRequest;
import com.matheusdev.mindforge.study.subject.dto.SubjectResponse;
import com.matheusdev.mindforge.study.service.StudyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.matheusdev.mindforge.study.subject.dto.SubjectSummaryResponse;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/studies")
@RequiredArgsConstructor
@Tag(name = "Studies", description = "Study session and subject management")
public class StudyRestController {

    private final StudyService service;

    @Operation(summary = "Get all subjects", description = "Returns a paginated list of study subjects, optionally filtered by workspace")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the list")
    })
    @GetMapping("/subjects")
    public ResponseEntity<Page<SubjectSummaryResponse>> getAllSubjects(
            @RequestParam(required = false) Long workspaceId,
            @ParameterObject Pageable pageable) {
        if (workspaceId != null) {
            return ResponseEntity.ok(service.getSubjectsByWorkspaceId(workspaceId, pageable));
        }
        // If no workspaceId provided, return all subjects for the tenant
        return ResponseEntity.ok(service.getAllSubjectsSummary(pageable));
    }

    @Operation(summary = "Get a subject by ID", description = "Returns a single study subject")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the subject"),
            @ApiResponse(responseCode = "404", description = "Subject not found")
    })
    @GetMapping("/subjects/{subjectId}")
    public ResponseEntity<SubjectResponse> getSubjectById(@PathVariable Long subjectId) {
        return ResponseEntity.ok(service.getSubjectById(subjectId));
    }

    @Operation(summary = "Create a new subject", description = "Creates a new study subject")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully created the subject")
    })
    @PostMapping("/subjects")
    public ResponseEntity<SubjectResponse> createSubject(@RequestBody @Valid SubjectRequest request) {
        return ResponseEntity.ok(service.createSubject(request));
    }

    @Operation(summary = "Update a subject", description = "Updates an existing study subject")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated the subject"),
            @ApiResponse(responseCode = "404", description = "Subject not found")
    })
    @PutMapping("/subjects/{subjectId}")
    public ResponseEntity<SubjectResponse> updateSubject(@PathVariable Long subjectId,
            @RequestBody @Valid SubjectRequest request) {
        return ResponseEntity.ok(service.updateSubject(subjectId, request));
    }

    @Operation(summary = "Delete a subject", description = "Deletes a study subject")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully deleted the subject"),
            @ApiResponse(responseCode = "404", description = "Subject not found")
    })
    @DeleteMapping("/subjects/{subjectId}")
    public ResponseEntity<Void> deleteSubject(@PathVariable Long subjectId) {
        service.deleteSubject(subjectId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all study sessions", description = "Returns all study sessions for the current tenant across all subjects")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all sessions")
    })
    @GetMapping("/sessions")
    public ResponseEntity<List<StudySessionResponse>> getAllSessions() {
        return ResponseEntity.ok(service.getAllSessions());
    }

    @Operation(summary = "Get all sessions for a subject", description = "Returns all study sessions for a specific subject")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the sessions")
    })
    @GetMapping("/subjects/{subjectId}/sessions")
    public ResponseEntity<List<StudySessionResponse>> getSessionsBySubject(@PathVariable Long subjectId) {
        return ResponseEntity.ok(service.getSessionsBySubject(subjectId));
    }

    @Operation(summary = "Get a study session by ID", description = "Returns a single study session")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the session"),
            @ApiResponse(responseCode = "404", description = "Session not found")
    })
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<StudySessionResponse> getSessionById(@PathVariable Long sessionId) {
        return ResponseEntity.ok(service.getSessionById(sessionId));
    }

    @Operation(summary = "Log a study session", description = "Logs a new study session for a specific subject")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully logged the session")
    })
    @PostMapping("/subjects/{subjectId}/sessions")
    public ResponseEntity<StudySessionResponse> logSession(
            @PathVariable Long subjectId,
            @RequestBody @Valid StudySessionRequest request) {
        return ResponseEntity.ok(service.logSession(subjectId, request));
    }

    @Operation(summary = "Update a study session", description = "Updates an existing study session")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated the session"),
            @ApiResponse(responseCode = "404", description = "Session not found")
    })
    @PutMapping("/sessions/{sessionId}")
    public ResponseEntity<StudySessionResponse> updateSession(
            @PathVariable Long sessionId,
            @RequestBody @Valid StudySessionRequest request) {
        return ResponseEntity.ok(service.updateSession(sessionId, request));
    }

    @Operation(summary = "Delete a study session", description = "Deletes a study session")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully deleted the session"),
            @ApiResponse(responseCode = "404", description = "Session not found")
    })
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long sessionId) {
        service.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
