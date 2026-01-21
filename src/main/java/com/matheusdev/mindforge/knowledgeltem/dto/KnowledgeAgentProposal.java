package com.matheusdev.mindforge.knowledgeltem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeAgentProposal {
    private Long knowledgeId;
    private String proposalId; // UUID for tracking
    private List<ContentChange> changes;
    private String summary; // AI-generated summary of changes
    private LocalDateTime createdAt;
    private String originalContent; // For reference
}
