package com.matheusdev.mindforge.study.subject.dto;

import com.matheusdev.mindforge.study.subject.model.enums.ProficiencyLevel;
import lombok.Data;

@Data
public class SubjectSummaryResponse {
    private Long id;
    private String name;
    private String description;
    private String proficiencyLevel;
    private String githubRepoUrl;
    private Integer sessionCount;
}
