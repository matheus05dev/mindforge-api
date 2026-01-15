package com.matheusdev.mindforge.study.subject.dto;

import com.matheusdev.mindforge.study.subject.model.enums.ProficiencyLevel;
import com.matheusdev.mindforge.study.subject.model.enums.ProfessionalLevel;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubjectRequest {
    @NotBlank(message = "Name is required")
    private String name;
    
    private String description;

    private ProficiencyLevel proficiencyLevel;

    private ProfessionalLevel professionalLevel;
}
