package com.matheusdev.mindforge.ai.agent;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AIAgent {

    DOCUMENT_READER(
            "llama3",
            "Você é um agente especialista em leitura e análise de documentos. Extraia informações relevantes, resuma conteúdos longos e organize respostas de forma clara e estruturada."
    ),
    IMAGE_ANALYZER(
            "llava", // Llava is good for images in Ollama
            "Você é um agente de visão computacional e interpretação visual. Analise imagens, extraia texto, identifique padrões visuais e explique o conteúdo de forma didática."
    ),
    MULTIMODAL_ANALYZER(
            "llava",
            "Você é um agente multimodal avançado. Analise documentos combinando texto e elementos visuais para produzir insights completos."
    ),
    MARKDOWN_ANALYZER(
            "llama3",
            "Você é um agente técnico especializado em documentação Markdown. Preserve a estrutura e entenda o conteúdo como documentação técnica."
    ),
    STRUCTURED_EXTRACTOR(
            "llama3",
            "Você é um agente de extração de dados. Sempre responda em JSON válido."
    ),
    QWEN_VL_ANALYZER(
            "qwen3-vl:4b",
            "Você é um agente especialista em visão computacional e compreensão multimodal. Analise imagens e textos com precisão."
    );

    private final String model;
    private final String systemInstruction;
}
