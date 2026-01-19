package com.matheusdev.mindforge.ai.dto;

public record ChatResponseDTO(
    Object content,
    String type // MARKDOWN | TEXT | ERROR
) {}
