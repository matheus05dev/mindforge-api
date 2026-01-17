package com.matheusdev.mindforge.ai.agent;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AIAgent {

    DOCUMENT_READER(
            "gemini-2.0-flash",
            "Você é um agente especialista em leitura e análise de documentos. Extraia informações relevantes, resuma conteúdos longos e organize respostas de forma clara e estruturada."
    ),
    IMAGE_ANALYZER(
            "gemini-2.0-flash",
            "Você é um agente de visão computacional e interpretação visual. Analise imagens, extraia texto, identifique padrões visuais e explique o conteúdo de forma didática."
    ),
    MULTIMODAL_ANALYZER(
            "gemini-2.0-flash",
            "Você é um agente multimodal avançado. Analise documentos combinando texto e elementos visuais para produzir insights completos."
    ),
    MARKDOWN_ANALYZER(
            "gemini-2.0-flash",
            "Você é um agente técnico especializado em documentação Markdown. Preserve a estrutura e entenda o conteúdo como documentação técnica."
    ),
    STRUCTURED_EXTRACTOR(
            "gemini-2.0-flash",
            "Você é um agente de extração de dados. Sempre responda em JSON válido."
    );

    private final String model;
    private final String systemInstruction;
}
