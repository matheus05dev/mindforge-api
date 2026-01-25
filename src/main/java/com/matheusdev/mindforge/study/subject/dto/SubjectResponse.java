package com.matheusdev.mindforge.study.subject.dto;

import com.matheusdev.mindforge.study.dto.StudySessionResponse;
import com.matheusdev.mindforge.study.subject.model.enums.ProficiencyLevel;
import lombok.Data;
import java.util.List;

@Data
public class SubjectResponse {
    private Long id;
    private String name;
    private String description;
    private String proficiencyLevel;
    private String githubRepoUrl;
    private Long workspaceId;
    private List<StudySessionResponse> studySessions;
}
