package com.matheusdev.mindforge.ai.dto;

import lombok.Data;

@Data
public class CodeAnalysisRequest {

    public enum AnalysisMode {
        MENTOR, // Modo didático e guiado (padrão)
        ANALYST, // Modo direto e sincero
        DEBUG_ASSISTANT, // Focado em encontrar e corrigir bugs
        SOCRATIC_TUTOR // Focado em fazer perguntas para guiar o aprendizado
    }

    private Long subjectId; // Contexto do estudo
    private String codeToAnalyze;
    private Long documentId; // Alternativa: analisar um documento já enviado
    private AnalysisMode mode = AnalysisMode.MENTOR; // Define MENTOR como padrão
}
