package com.matheusdev.mindforge.knowledgeltem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KnowledgeAIResponse {
    private String result; // For THINKING mode (direct text result)
    private KnowledgeAgentProposal proposal; // For AGENT mode (structured diff proposal)
    private boolean success;
    private String message;

    // Constructor for THINKING mode
    public KnowledgeAIResponse(String result, boolean success, String message) {
        this.result = result;
        this.proposal = null;
        this.success = success;
        this.message = message;
    }
}
