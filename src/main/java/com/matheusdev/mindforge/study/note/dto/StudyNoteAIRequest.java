package com.matheusdev.mindforge.study.note.dto;

import lombok.Data;

@Data
public class StudyNoteAIRequest {
    public enum Command {
        CONTINUE,
        SUMMARIZE,
        FIX_GRAMMAR,
        IMPROVE,
        CUSTOM,
        ASK_AGENT,
        AGENT_UPDATE
    }

    private Command command;
    private String context; // The current note content
    private String instruction; // Extra instruction
    private boolean useContext; // context RAG
    private Long noteId; // Note ID
    private boolean agentMode;
}
