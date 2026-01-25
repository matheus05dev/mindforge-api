package com.matheusdev.mindforge.project.dto;

import lombok.Data;

@Data
public class ProjectSummaryResponse {
    private Long id;
    private String name;
    private String description;
    private Long workspaceId;
    private String githubRepoUrl;
    private int milestoneCount;
    private int documentCount;
}
