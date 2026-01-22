package com.matheusdev.mindforge.project.decision.dto;

import com.matheusdev.mindforge.project.decision.model.DecisionStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DecisionResponse {
    private Long id;
    private Long projectId;
    private String title;
    private DecisionStatus status;
    private String context;
    private String decision;
    private String consequences;
    private List<String> tags;
    private String author;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
