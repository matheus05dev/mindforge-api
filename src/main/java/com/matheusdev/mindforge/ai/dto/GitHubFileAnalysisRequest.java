package com.matheusdev.mindforge.ai.dto;

import lombok.Data;

@Data
public class GitHubFileAnalysisRequest {
    private Long projectId;
    private Long subjectId;
    private String filePath; // Ex: "src/main/java/com/matheusdev/mindforge/ai/service/AIService.java"
    private CodeAnalysisRequest.AnalysisMode mode = CodeAnalysisRequest.AnalysisMode.MENTOR;
}
