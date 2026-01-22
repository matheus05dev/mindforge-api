package com.matheusdev.mindforge.study.resource.dto;

import com.matheusdev.mindforge.study.resource.model.StudyResource;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudyResourceResponse {
    private Long id;
    private Long subjectId;
    private String title;
    private StudyResource.ResourceType type;
    private String url;
    private String description;
    private Boolean isAIGenerated;
    private LocalDateTime createdAt;
}
