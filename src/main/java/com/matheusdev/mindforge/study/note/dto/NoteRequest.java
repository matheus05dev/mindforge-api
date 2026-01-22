package com.matheusdev.mindforge.study.note.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NoteRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String content;

    private String tags;
}
