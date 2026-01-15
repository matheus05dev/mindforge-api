package com.matheusdev.mindforge.ai.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenericAnalysisRequest {
    private String question;
    private Long subjectId;
    private Long projectId;
    private String provider; // "gemini", "groq" ou nulo para "mindforge" (padrão)
}
