package com.matheusdev.mindforge.knowledgeltem.dto;

import lombok.Data;

@Data
public class KnowledgeAIRequest {
    public enum Command {
        CONTINUE, // Continua o texto atual
        SUMMARIZE, // Resuma o conteúdo
        FIX_GRAMMAR, // Corrige gramática
        IMPROVE, // Melhora a escrita
        CUSTOM, // Instrução customizada (ex: "Traduza para inglês")
        ASK_AGENT, // Pergunta ao agente (RAG/Contexto Global)
        AGENT_UPDATE // Agent proposes structured changes (diff-based)
    }

    private Command command;
    private String context; // O conteúdo atual da nota
    private String instruction; // Instrução extra (para CUSTOM ou ASK_AGENT)
    private boolean useContext; // Se true, ativa RAG/Busca em outras notas
    private Long knowledgeId; // ID da nota para persistência do chat
    private boolean agentMode; // If true, return proposal instead of direct result
}
