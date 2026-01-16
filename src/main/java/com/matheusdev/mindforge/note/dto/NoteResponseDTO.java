package com.matheusdev.mindforge.note.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NoteResponseDTO {
    private Long id;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
