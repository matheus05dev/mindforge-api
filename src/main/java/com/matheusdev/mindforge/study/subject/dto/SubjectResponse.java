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
    private ProficiencyLevel proficiencyLevel;
    private List<StudySessionResponse> studySessions;
}
