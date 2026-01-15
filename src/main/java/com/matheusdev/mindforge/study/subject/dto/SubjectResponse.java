package com.matheusdev.mindforge.study.subject.dto;

import com.matheusdev.mindforge.study.dto.StudySessionResponse;
import com.matheusdev.mindforge.study.subject.model.enums.ProficiencyLevel;
import com.matheusdev.mindforge.study.subject.model.enums.ProfessionalLevel;
import lombok.Data;
import java.util.List;

@Data
public class SubjectResponse {
    private Long id;
    private String name;
    private String description;
    private ProficiencyLevel proficiencyLevel;
    private ProfessionalLevel professionalLevel;
    private List<StudySessionResponse> studySessions;
}
