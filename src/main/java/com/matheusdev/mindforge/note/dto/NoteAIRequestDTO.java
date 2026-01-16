package com.matheusdev.mindforge.note.dto;

import lombok.Data;

@Data
public class NoteAIRequestDTO {
    private String instruction; // e.g., "Summarize this", "Fix grammar", "Expand on this"
}
