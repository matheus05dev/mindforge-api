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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/studies")
@RequiredArgsConstructor
@Tag(name = "Studies", description = "Study session and subject management")
public class StudyRestController {

    private final StudyService service;

    @Operation(summary = "Get all subjects", description = "Returns a list of all study subjects")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the list")
    })
    @GetMapping("/subjects")
    public ResponseEntity<List<SubjectResponse>> getAllSubjects() {
        return ResponseEntity.ok(service.getAllSubjects());
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
    public ResponseEntity<SubjectResponse> updateSubject(@PathVariable Long subjectId, @RequestBody @Valid SubjectRequest request) {
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
