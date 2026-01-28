package com.matheusdev.mindforge.study.note.api;

import com.matheusdev.mindforge.study.note.dto.NoteRequest;
import com.matheusdev.mindforge.study.note.dto.NoteResponse;
import com.matheusdev.mindforge.study.note.service.StudyNoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/studies")
@RequiredArgsConstructor
public class StudyNoteRestController {

    private final StudyNoteService noteService;
    private final com.matheusdev.mindforge.knowledgeltem.service.ProposalCacheService proposalCacheService;

    @GetMapping("/subjects/{subjectId}/notes")
    public ResponseEntity<List<NoteResponse>> getNotesBySubject(@PathVariable Long subjectId) {
        return ResponseEntity.ok(noteService.getNotesBySubject(subjectId));
    }

    @GetMapping("/notes")
    public ResponseEntity<List<NoteResponse>> getAllNotes() {
        return ResponseEntity.ok(noteService.getAllNotes());
    }

    @GetMapping("/notes/{noteId}")
    public ResponseEntity<NoteResponse> getNoteById(@PathVariable Long noteId) {
        return ResponseEntity.ok(noteService.getNoteById(noteId));
    }

    @PostMapping("/subjects/{subjectId}/notes")
    public ResponseEntity<NoteResponse> createNote(
            @PathVariable Long subjectId,
            @Valid @RequestBody NoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(noteService.createNote(subjectId, request));
    }

    @PutMapping("/notes/{noteId}")
    public ResponseEntity<NoteResponse> updateNote(
            @PathVariable Long noteId,
            @Valid @RequestBody NoteRequest request) {
        return ResponseEntity.ok(noteService.updateNote(noteId, request));
    }

    @DeleteMapping("/notes/{noteId}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long noteId) {
        noteService.deleteNote(noteId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/notes/ai")
    public CompletableFuture<ResponseEntity<com.matheusdev.mindforge.study.note.dto.StudyNoteAIResponse>> processAIRequest(
            @Valid @RequestBody com.matheusdev.mindforge.study.note.dto.StudyNoteAIRequest request) {
        return noteService.processAIRequest(request)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/notes/ai/proposals/{proposalId}/apply")
    public ResponseEntity<NoteResponse> applyProposal(
            @PathVariable String proposalId,
            @RequestBody com.matheusdev.mindforge.knowledgeltem.dto.ApprovalRequest approval) {

        // 1. Obter proposta do cache
        com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAgentProposal proposal = proposalCacheService
                .getProposal(proposalId);

        if (proposal == null) {
            return ResponseEntity.notFound().build();
        }

        // 2. Aplicar mudanças via service
        NoteResponse updated = noteService.applyProposal(proposal, approval);

        // 3. Remover proposta do cache
        proposalCacheService.removeProposal(proposalId);

        return ResponseEntity.ok(updated);
    }

    @GetMapping("/notes/{noteId}/versions")
    public ResponseEntity<List<com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeVersionResponse>> getVersions(
            @PathVariable Long noteId) {
        return ResponseEntity.ok(noteService.getVersionHistory(noteId));
    }

    @GetMapping("/notes/{noteId}/versions/{versionId}")
    public ResponseEntity<com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeVersionResponse> getVersion(
            @PathVariable Long noteId,
            @PathVariable Long versionId) {
        return ResponseEntity.ok(noteService.getVersion(versionId));
    }

    @PostMapping("/notes/{noteId}/versions/{versionId}/rollback")
    public ResponseEntity<NoteResponse> rollbackToVersion(
            @PathVariable Long noteId,
            @PathVariable Long versionId) {
        return ResponseEntity.ok(noteService.rollbackToVersion(noteId, versionId));
    }
}
