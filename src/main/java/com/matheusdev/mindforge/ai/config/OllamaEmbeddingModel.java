package com.matheusdev.mindforge.ai.config;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementação de EmbeddingModel do LangChain4j que usa a API do Ollama diretamente.
 * Não depende do Spring AI EmbeddingClient.
 */
@Component("langchain4jOllamaEmbeddingModel")
@Slf4j
public class OllamaEmbeddingModel implements EmbeddingModel {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String ollamaBaseUrl;
    private final String embeddingModel;
    private Integer cachedDimension;

    public OllamaEmbeddingModel(
            @Value("${ollama.embedding.base-url:http://localhost:11434}") String ollamaBaseUrl,
            @Value("${ollama.embedding.model:nomic-embed-text}") String embeddingModel) {
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.embeddingModel = embeddingModel;
        log.info("OllamaEmbeddingModel configurado: baseUrl={}, model={}", ollamaBaseUrl, embeddingModel);
    }

    @Override
    public Response<Embedding> embed(String text) {
        try {
            float[] embedding = callOllamaEmbedding(text);
            return Response.from(new Embedding(embedding));
        } catch (Exception e) {
            log.error("Erro ao gerar embedding para texto: {}", text, e);
            throw new RuntimeException("Falha ao gerar embedding", e);
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

            List<Embedding> embeddings = texts.stream()
                    .map(this::callOllamaEmbedding)
                    .map(Embedding::new)
                    .collect(Collectors.toList());

            return Response.from(embeddings);
        } catch (Exception e) {
            log.error("Erro ao gerar embeddings para {} segmentos", textSegments.size(), e);
            throw new RuntimeException("Falha ao gerar embeddings", e);
        }
    }

    @Override
    public int dimension() {
        if (cachedDimension != null) {
            return cachedDimension;
        }
        try {
            float[] testEmbedding = callOllamaEmbedding("test");
            cachedDimension = testEmbedding.length;
            return cachedDimension;
        } catch (Exception e) {
            log.warn("Não foi possível determinar a dimensão do embedding, usando 768 como padrão", e);
            return 768; // Dimensão comum para nomic-embed-text
        }
    }

    private float[] callOllamaEmbedding(String text) {
        try {
            String url = ollamaBaseUrl + "/api/embeddings";
            
            Map<String, Object> request = Map.of(
                    "model", embeddingModel,
                    "prompt", text
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            if (response == null || !response.containsKey("embedding")) {
                throw new RuntimeException("Resposta inválida do Ollama: " + response);
            }

            @SuppressWarnings("unchecked")
            List<Double> embeddingDoubles = (List<Double>) response.get("embedding");

            float[] embedding = new float[embeddingDoubles.size()];
            for (int i = 0; i < embeddingDoubles.size(); i++) {
                embedding[i] = embeddingDoubles.get(i).floatValue();
            }

            return embedding;
        } catch (Exception e) {
            log.error("Erro ao chamar API de embeddings do Ollama: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao chamar API do Ollama para embeddings", e);
        }
    }
}

