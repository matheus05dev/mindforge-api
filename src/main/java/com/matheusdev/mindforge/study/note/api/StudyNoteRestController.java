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

@RestController
@RequestMapping("/api/studies")
@RequiredArgsConstructor
public class StudyNoteRestController {

    private final StudyNoteService noteService;

    @GetMapping("/subjects/{subjectId}/notes")
    public ResponseEntity<List<NoteResponse>> getNotesBySubject(@PathVariable Long subjectId) {
        return ResponseEntity.ok(noteService.getNotesBySubject(subjectId));
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
}
