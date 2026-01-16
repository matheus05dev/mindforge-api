package com.matheusdev.mindforge.project.dto;

import com.matheusdev.mindforge.document.dto.DocumentResponse;
import com.matheusdev.mindforge.project.milestone.dto.MilestoneResponse;
import lombok.Data;

import java.util.List;

@Data
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private Long workspaceId;
    private String githubRepoUrl;
    private List<DocumentResponse> documents;
    private List<MilestoneResponse> milestones;
}
