package com.matheusdev.mindforge.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Smart Router que decide a estratégia de processamento de documentos
 * baseado no tamanho do documento e no custo computacional.
 * 
 * Estratégias:
 * - Até 10k chars: One-shot (envio direto)
 * - 10k a 100k chars: Map-Reduce (análise por chunks)
 * - Acima de 100k chars: RAG (busca semântica com vector store)
 */
@Service
@Slf4j
public class SmartRouterService {

    private static final int ONE_SHOT_THRESHOLD = 10_000;  // 10k caracteres
    private static final int MAP_REDUCE_THRESHOLD = 100_000; // 100k caracteres
    private static final int RAG_FOR_MEDIUM_THRESHOLD = 50_000; // 50k caracteres - usar RAG para evitar rate limit
    private static final int ESTIMATED_CHUNKS_PER_10K = 4; // Estimativa: ~4 chunks por 10k chars (com chunk de 2500 tokens)

    public enum ProcessingStrategy {
        ONE_SHOT,      // Envio direto para documentos pequenos
        MAP_REDUCE,    // Map-Reduce para documentos médios
        RAG            // RAG para documentos grandes ou quando Map-Reduce geraria muitos chunks
    }

    /**
     * Decide a estratégia de processamento baseado no tamanho do documento.
     * 
     * @param documentLength Tamanho do documento em caracteres
     * @return Estratégia de processamento recomendada
     */
    public ProcessingStrategy decideStrategy(int documentLength) {
        ProcessingStrategy strategy;
        
        if (documentLength <= ONE_SHOT_THRESHOLD) {
            strategy = ProcessingStrategy.ONE_SHOT;
            log.info("🤖 Smart Router: Documento PEQUENO ({} chars <= {}) -> Estratégia: ONE-SHOT (Envio Direto)", 
                    documentLength, ONE_SHOT_THRESHOLD);
        } else if (documentLength > MAP_REDUCE_THRESHOLD) {
            strategy = ProcessingStrategy.RAG;
            log.info("🤖 Smart Router: Documento GRANDE ({} chars > {}) -> Estratégia: RAG (Busca Semântica)", 
                    documentLength, MAP_REDUCE_THRESHOLD);
        } else {
            // Para documentos médios, estima quantos chunks seriam gerados
            int estimatedChunks = (documentLength / 10_000) * ESTIMATED_CHUNKS_PER_10K;
            
            // Se o documento é médio-grande (> 50k) ou geraria muitos chunks (> 15), usa RAG
            // para evitar rate limit e reduzir número de requisições
            if (documentLength >= RAG_FOR_MEDIUM_THRESHOLD || estimatedChunks > 15) {
                strategy = ProcessingStrategy.RAG;
                log.info("🤖 Smart Router: Documento MÉDIO-GRANDE ({} chars, ~{} chunks estimados) -> Estratégia: RAG (Evita Rate Limit)", 
                        documentLength, estimatedChunks);
            } else {
                strategy = ProcessingStrategy.MAP_REDUCE;
                log.info("🤖 Smart Router: Documento MÉDIO ({} chars, ~{} chunks estimados) -> Estratégia: MAP-REDUCE (Análise por Chunks)", 
                        documentLength, estimatedChunks);
            }
        }
        
        return strategy;
    }

    /**
     * Verifica se o documento é pequeno o suficiente para processamento one-shot.
     */
    public boolean isOneShot(int documentLength) {
        return documentLength <= ONE_SHOT_THRESHOLD;
    }

    /**
     * Verifica se o documento requer processamento Map-Reduce.
     */
    public boolean requiresMapReduce(int documentLength) {
        return documentLength > ONE_SHOT_THRESHOLD && documentLength <= MAP_REDUCE_THRESHOLD;
    }

    /**
     * Verifica se o documento requer processamento RAG.
     */
    public boolean requiresRAG(int documentLength) {
        return documentLength > MAP_REDUCE_THRESHOLD;
    }
}

