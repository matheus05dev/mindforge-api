package com.matheusdev.mindforge.knowledgeltem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentChange {
    public enum ChangeType {
        ADD, // Add new content
        REMOVE, // Remove existing content
        REPLACE // Replace existing content with new
    }

    private ChangeType type;
    private Integer startLine;
    private Integer endLine;
    private String originalContent;
    private String proposedContent;
    private String reason; // Why this change was proposed
}
