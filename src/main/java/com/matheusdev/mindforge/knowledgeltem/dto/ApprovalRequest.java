package com.matheusdev.mindforge.knowledgeltem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequest {
    private List<Integer> approvedChangeIndices; // Indices of approved changes, or empty for "approve all"
    private boolean approveAll; // If true, apply all changes
    private String userComment; // Optional comment from user
    private java.util.Map<Integer, String> modifiedContent; // Optional manual edits: Index -> New Content
}
