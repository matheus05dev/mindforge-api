package com.matheusdev.mindforge.study.note.dto;

import com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAgentProposal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudyNoteAIResponse {
    private String result;
    private boolean success;
    private String message;
    private KnowledgeAgentProposal proposal; // We can reuse the proposal DTO or create a new one if needed

    public StudyNoteAIResponse(String result, boolean success, String message) {
        this.result = result;
        this.success = success;
        this.message = message;
    }
}
