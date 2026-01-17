package com.matheusdev.mindforge.ai.config;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Adapter que converte Spring AI EmbeddingClient para langchain4j EmbeddingModel
 * Permite usar Spring AI embeddings mantendo compatibilidade com RAGService
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EmbeddingAdapter implements EmbeddingModel {

    private final org.springframework.ai.embedding.EmbeddingModel embeddingModel;

    @Override
    public Response<Embedding> embed(String text) {
        try {
            float[] embeddingVector = embeddingModel.embed(text);
            Embedding embedding = new Embedding(embeddingVector);
            return Response.from(embedding);
        } catch (Exception e) {
            log.error("Error generating embedding for text: {}", text, e);
            throw new RuntimeException("Failed to generate embedding", e);
        }
    }

    @Override
    public Response<Embedding> embed(TextSegment textSegment) {
        return embed(textSegment.text());
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        try {
            List<String> texts = textSegments.stream()
                    .map(TextSegment::text)
                    .collect(Collectors.toList());

            List<float[]> embeddingVectors = embeddingModel.embed(texts);

            List<Embedding> embeddings = embeddingVectors.stream()
                    .map(Embedding::new)
                    .collect(Collectors.toList());

            return Response.from(embeddings);
        } catch (Exception e) {
            log.error("Error generating embeddings for {} segments", textSegments.size(), e);
            throw new RuntimeException("Failed to generate embeddings", e);
        }
    }

    @Override
    public int dimension() {
        try {
            float[] testEmbedding = embeddingModel.embed("test");
            return testEmbedding != null ? testEmbedding.length : 384;
        } catch (Exception e) {
            log.warn("Could not determine embedding dimension, defaulting to 384", e);
            return 384; // Common dimension for small models like all-MiniLM-L6-v2
        }
    }
}
