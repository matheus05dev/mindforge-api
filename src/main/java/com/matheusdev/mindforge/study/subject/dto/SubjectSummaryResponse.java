package com.matheusdev.mindforge.study.subject.dto;

import com.matheusdev.mindforge.study.subject.model.enums.ProficiencyLevel;
import lombok.Data;

@Data
public class SubjectSummaryResponse {
    private Long id;
    private String name;
    private String description;
    private ProficiencyLevel proficiencyLevel;
    private int sessionCount;
}
