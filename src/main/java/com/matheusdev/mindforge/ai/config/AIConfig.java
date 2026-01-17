package com.matheusdev.mindforge.ai.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuração de modelos de embedding usando Spring AI
 * Remove dependência do HuggingFace que estava com problemas de URL
 */
@Configuration
@RequiredArgsConstructor
public class AIConfig {

    private final EmbeddingAdapter embeddingAdapter;

    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        // Usa Spring AI EmbeddingClient através do adapter
        // Configuração da API está em application.properties:
        // spring.ai.openai.api-key=...
        // spring.ai.openai.embedding.options.model=text-embedding-3-small
        return embeddingAdapter;
    }
}
