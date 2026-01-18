package com.matheusdev.mindforge.ai.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuração de modelos de embedding usando Ollama via LangChain4j.
 * Usa OllamaEmbeddingModel que chama a API do Ollama diretamente.
 */
@Configuration
@RequiredArgsConstructor
public class AIConfig {

    private final OllamaEmbeddingModel ollamaEmbeddingModel;

    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        // Usa OllamaEmbeddingModel que chama a API do Ollama diretamente
        // Configuração está em application.properties:
        // ollama.embedding.base-url=http://localhost:11434
        // ollama.embedding.model=nomic-embed-text
        return ollamaEmbeddingModel;
    }
}
