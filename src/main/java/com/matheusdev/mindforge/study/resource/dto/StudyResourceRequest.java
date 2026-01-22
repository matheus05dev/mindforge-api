package com.matheusdev.mindforge.study.resource.dto;

import com.matheusdev.mindforge.study.resource.model.StudyResource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StudyResourceRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Type is required")
    private StudyResource.ResourceType type;

    @NotBlank(message = "URL is required")
    private String url;

    private String description;

    private Boolean isAIGenerated = false;
}
