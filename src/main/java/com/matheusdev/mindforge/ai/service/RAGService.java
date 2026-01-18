package com.matheusdev.mindforge.ai.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RAGService {

    private final VectorStoreService vectorStoreService;
    private final DocumentAnalyzer documentAnalyzer;

    /**
     * Processa query com RAG ADAPTATIVO.
     * MinScore e maxResults ajustados automaticamente baseado no documento.
     */
    public List<TextSegment> processQueryWithRAG(String documentId, Document document, String query, int maxResults) {
        log.info("🚀 Iniciando RAG ADAPTATIVO para '{}' com query: '{}'", documentId, query);

        // Indexar documento (ou reutilizar se já indexado)
        vectorStoreService.getOrCreateVectorStore(documentId, document);

        // Analisar documento para ajustar estratégia de busca
        DocumentAnalyzer.DocumentProfile profile = documentAnalyzer.analyzeDocument(document.text());
        
        // Calcular minScore adaptativo
        double minScore = calculateAdaptiveMinScore(profile, query);
        
        // Ajustar maxResults baseado na complexidade
        int adaptiveMaxResults = calculateAdaptiveMaxResults(profile, maxResults);
        
        log.info("📊 Busca adaptativa: maxResults={}, minScore={}, docType={}", 
                adaptiveMaxResults, minScore, profile.type);

        // Buscar segmentos
        List<TextSegment> relevantSegments = vectorStoreService.findRelevantSegments(
            documentId, query, adaptiveMaxResults, minScore
        );

        if (relevantSegments.isEmpty()) {
            log.warn("⚠️ Nenhum segmento relevante encontrado para query '{}'.", query);
        } else {
            log.info("✅ RAG processado. {} segmentos relevantes encontrados.", relevantSegments.size());
        }

        return relevantSegments;
    }

    /**
     * Calcula minScore adaptativo baseado no perfil do documento e tipo de query.
     */
    private double calculateAdaptiveMinScore(DocumentAnalyzer.DocumentProfile profile, String query) {
        double baseScore = 0.7; // Score padrão

        // Documentos complexos: score mais permissivo
        if (profile.complexity == DocumentAnalyzer.ComplexityLevel.HIGH) {
            baseScore = 0.65;
        }

        // Documentos grandes: score ligeiramente mais baixo
        if (profile.length > 50_000) {
            baseScore -= 0.05;
        }

        // Queries longas/específicas: pode aumentar score
        if (query.length() > 100) {
            baseScore += 0.05;
        }

        // Garantir limites
        return Math.max(0.5, Math.min(0.8, baseScore));
    }

    /**
     * Calcula maxResults adaptativo baseado no perfil, otimizado para economia de tokens.
     */
    private int calculateAdaptiveMaxResults(DocumentAnalyzer.DocumentProfile profile, int requestedMax) {
        int adaptive = requestedMax;

        // Para documentos complexos, usar um valor moderado
        if (profile.complexity == DocumentAnalyzer.ComplexityLevel.HIGH) {
            adaptive = Math.max(requestedMax, 8); // Reduzido de 15 para 8
        }

        // Para documentos muito longos, aumentar um pouco, mas com moderação
        if (profile.length > 100_000) {
            adaptive = Math.max(adaptive, 10); // Reduzido de 20 para 10
        }

        // Teto geral mais baixo para garantir que não exceda os limites do free tier
        return Math.min(adaptive, 12); // Reduzido de 25 para 12
    }

    public void indexDocument(String documentId, Document document) {
        log.info("📥 Indexando documento '{}'...", documentId);
        vectorStoreService.getOrCreateVectorStore(documentId, document);
        log.info("✅ Documento '{}' indexado.", documentId);
    }

    public List<TextSegment> queryIndexedDocument(String documentId, String query, int maxResults) {
        return vectorStoreService.findRelevantSegments(documentId, query, maxResults, 0.7);
    }
}
