package com.matheusdev.mindforge.note.api;

import com.matheusdev.mindforge.note.dto.NoteAIRequestDTO;
import com.matheusdev.mindforge.note.dto.NoteRequestDTO;
import com.matheusdev.mindforge.note.dto.NoteResponseDTO;
import com.matheusdev.mindforge.note.service.NoteAIService;
import com.matheusdev.mindforge.note.service.NoteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class NoteRestController {

    private final NoteService noteService;
    private final NoteAIService noteAIService;

    public NoteRestController(NoteService noteService, NoteAIService noteAIService) {
        this.noteService = noteService;
        this.noteAIService = noteAIService;
    }

    @PostMapping
    public ResponseEntity<NoteResponseDTO> createNote(@RequestBody NoteRequestDTO requestDTO) {
        return new ResponseEntity<>(noteService.createNote(requestDTO), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<NoteResponseDTO>> getAllNotes() {
        return ResponseEntity.ok(noteService.getAllNotes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoteResponseDTO> getNoteById(@PathVariable Long id) {
        return ResponseEntity.ok(noteService.getNoteById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoteResponseDTO> updateNote(@PathVariable Long id, @RequestBody NoteRequestDTO requestDTO) {
        return ResponseEntity.ok(noteService.updateNote(id, requestDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id) {
        noteService.deleteNote(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/ai")
    public ResponseEntity<NoteResponseDTO> processNoteWithAI(@PathVariable Long id, @RequestBody NoteAIRequestDTO requestDTO) {
        return ResponseEntity.ok(noteAIService.processNoteWithAI(id, requestDTO));
    }
}
