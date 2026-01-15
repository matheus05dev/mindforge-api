package com.matheusdev.mindforge.study.dto;

import com.matheusdev.mindforge.document.dto.DocumentResponse;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class StudySessionResponse {
    private Long id;
    private Long subjectId;
    private String subjectName;
    private LocalDateTime startTime;
    private Integer durationMinutes;
    private String notes;
    private List<DocumentResponse> documents;
}
