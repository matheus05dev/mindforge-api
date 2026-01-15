package com.matheusdev.mindforge.project.milestone.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class MilestoneResponse {
    private Long id;
    private Long projectId;
    private String title;
    private String description;
    private LocalDate dueDate;
    private boolean completed;
}
