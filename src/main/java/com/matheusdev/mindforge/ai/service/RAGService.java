package com.matheusdev.mindforge.ai.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RAGService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingStore<TextSegment> createEmbeddingStore(String documentContent) {
        log.info("Iniciando processo de RAG: criando o embedding store.");

        // 1. Fatiar o documento em pedaços menores
        Document document = Document.from(documentContent);
        DocumentSplitter splitter = DocumentSplitters.recursive(500, 100);
        List<TextSegment> segments = splitter.split(document);
        log.info("{} segmentos de texto foram criados.", segments.size());

        // 2. Criar os embeddings para cada pedaço
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        log.info("Embeddings gerados para todos os segmentos.");

        // 3. Armazenar os embeddings em um banco de dados vetorial em memória
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.addAll(embeddings, segments);
        log.info("Embedding store criado e populado em memória.");

        return embeddingStore;
    }

    public List<TextSegment> findRelevantSegments(String query, EmbeddingStore<TextSegment> embeddingStore, int maxResults) {
        log.info("Buscando segmentos relevantes para a query: '{}'", query);
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        //requer um parâmetro 'minScore' para filtrar resultados por relevância.
        // Um valor de 0.7 é um padrão razoável para começar.
        List<EmbeddingMatch<TextSegment>> relevantEmbeddings = embeddingStore.findRelevant(queryEmbedding, maxResults, 0.7);

        log.info("Encontrados {} segmentos relevantes.", relevantEmbeddings.size());
        
        return relevantEmbeddings.stream()
                .map(EmbeddingMatch::embedded)
                .toList();
    }
}
