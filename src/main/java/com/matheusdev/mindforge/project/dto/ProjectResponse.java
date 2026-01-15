package com.matheusdev.mindforge.project.dto;

import com.matheusdev.mindforge.document.dto.DocumentResponse;
import lombok.Data;

import java.util.List;

@Data
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private List<DocumentResponse> documents;
}
