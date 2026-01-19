package com.matheusdev.mindforge.ai.splitter;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SemanticDocumentSplitter implements DocumentSplitter {

    private static final Logger log = LoggerFactory.getLogger(SemanticDocumentSplitter.class);

    private final EmbeddingModel embeddingModel;
    private final double similarityThreshold;
    private final int maxChunkSize;
    private final int minChunkSize;
    private final int overlap;

    public SemanticDocumentSplitter(EmbeddingModel embeddingModel, double similarityThreshold, int maxChunkSize,
            int minChunkSize, int overlap) {
        this.embeddingModel = embeddingModel;
        this.similarityThreshold = similarityThreshold;
        this.maxChunkSize = maxChunkSize;
        this.minChunkSize = minChunkSize;
        this.overlap = overlap;
    }

    @Override
    public List<TextSegment> split(Document document) {
        log.info("✂️ Iniciando Semantic Chunking...");
        String text = document.text();

        // 1. Quebra inicial simples por parágrafos ou quebras de linha duplas para
        // preservar tabelas/estrutura
        // Regex lookbehind para manter o delimitador se necessário, mas split simples
        // costuma funcionar bem para separar blocos lógicos
        String[] rawBlocks = text.split("(?<=\\n\\n)");

        List<TextSegment> segments = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int currentChunkSize = 0;
        float[] currentChunkEmbedding = null;

        for (String block : rawBlocks) {
            String cleanBlock = block.trim();
            if (cleanBlock.isEmpty())
                continue;

            // Se o bloco sozinho já é maior que o max, temos que forçar split (fallback
            // para caractere)
            if (cleanBlock.length() > maxChunkSize) {
                if (currentChunk.length() > 0) {
                    TextSegment segment = TextSegment.from(currentChunk.toString());
                    enrichSegmentWithMetadata(segment);
                    segments.add(segment);
                    currentChunk = new StringBuilder();
                    currentChunkSize = 0;
                    currentChunkEmbedding = null;
                }
                TextSegment largeBlockSegment = TextSegment.from(cleanBlock);
                enrichSegmentWithMetadata(largeBlockSegment);
                segments.add(largeBlockSegment); // Idealmente quebraria recursivamente aqui, por enquanto
                                                 // aceitamos
                continue;
            }

            // Se é o primeiro bloco do chunk
            if (currentChunk.length() == 0) {
                currentChunk.append(cleanBlock).append("\n\n");
                currentChunkSize += cleanBlock.length();
                // Opcional: Gerar embedding do bloco atual para comparar com o próximo
                // Para simplificar e economizar tokens, podemos usar heurística ou embedding
                // real
                // Vamos usar embedding real pois foi o pedido do user ("usar a msm ia do rag")
                try {
                    currentChunkEmbedding = embeddingModel.embed(cleanBlock).content().vector();
                } catch (Exception e) {
                    log.warn("Falha ao gerar embedding para chunking. Ignorando semântica neste bloco.");
                }
                continue;
            }

            // Comparar com o chunk atual
            boolean shouldMerge = true;
            if (currentChunkEmbedding != null) {
                try {
                    float[] nextBlockEmbedding = embeddingModel.embed(cleanBlock).content().vector();
                    double similarity = cosineSimilarity(currentChunkEmbedding, nextBlockEmbedding);

                    if (similarity < similarityThreshold) {
                        log.debug("📉 Quebra Semântica: Similaridade {} < {}", String.format("%.2f", similarity),
                                similarityThreshold);
                        shouldMerge = false;
                    }
                } catch (Exception e) {
                    // ignorar erro e mergear por default se couber
                }
            }

            // Verificação de tamanho
            if (currentChunkSize + cleanBlock.length() > maxChunkSize) {
                shouldMerge = false;
            }

            if (shouldMerge) {
                currentChunk.append(cleanBlock).append("\n\n");
                currentChunkSize += cleanBlock.length();
                // Opcional: Atualizar embedding do chunk (média) ou manter o do início?
                // Manter o do início (tópico âncora) costuma funcionar bem para estabilidade.
            } else {
                // Finaliza chunk atual
                String chunkText = currentChunk.toString().trim();
                TextSegment newSegment = TextSegment.from(chunkText);
                enrichSegmentWithMetadata(newSegment);
                segments.add(newSegment);

                // Prepara próximo chunk com overlap
                currentChunk = new StringBuilder();

                // Overlap: pega o final do chunk anterior
                if (overlap > 0 && chunkText.length() > overlap) {
                    // Tenta achar um ponto de corte seguro (espaço) para não cortar palavras
                    int startOverlap = chunkText.length() - overlap;
                    // Ajuste fino para não cortar no meio da palavra (busca espaço anterior)
                    int safeStart = chunkText.lastIndexOf(" ", startOverlap);
                    if (safeStart != -1) {
                        startOverlap = safeStart + 1;
                    }

                    String overlapText = chunkText.substring(startOverlap);
                    currentChunk.append(overlapText).append("\n... (continuação) ...\n");
                    currentChunkSize = overlapText.length();
                } else if (overlap > 0) { // Se o chunk for menor que o overlap, copia tudo
                    currentChunk.append(chunkText).append("\n... (continuação) ...\n");
                    currentChunkSize = chunkText.length();
                } else {
                    currentChunkSize = 0;
                }

                currentChunk.append(cleanBlock).append("\n\n");
                currentChunkSize += cleanBlock.length();
                try {
                    currentChunkEmbedding = embeddingModel.embed(cleanBlock).content().vector();
                } catch (Exception e) {
                    // log.warn("Embedding error for new chunk block", e);
                    currentChunkEmbedding = null;
                }
            }
        }

        if (currentChunk.length() > 0) {
            TextSegment finalSegment = TextSegment.from(currentChunk.toString().trim());
            enrichSegmentWithMetadata(finalSegment);
            segments.add(finalSegment);
        }

        log.info("✅ Semantic Chunking concluído. Gerados {} segmentos.", segments.size());
        return segments;
    }

    // --- Heuristic Analyzer Logic ---

    private void enrichSegmentWithMetadata(TextSegment segment) {
        if (HeuristicConceptExtractor.isCentralConcept(segment.text())) {
            segment.metadata().put("concept_centrality", "true");
        }
    }

    private static class HeuristicConceptExtractor {
        // Padrões de Definição (Forte Indício de Conteúdo Central)
        private static final java.util.regex.Pattern DEFINITION_PATTERN = java.util.regex.Pattern.compile(
                "(?i)(\\b(é|são|refere-se a|define-se como|significa)\\b|:\\s*$|^\\s*O que é)",
                java.util.regex.Pattern.MULTILINE);

        // Padrões de Listas Enumeradas (Componentes, Fatores)
        private static final java.util.regex.Pattern LIST_PATTERN = java.util.regex.Pattern.compile(
                "(?i)(\\b(consiste em|composto por|inclui|fatores|elementos|pilares)\\b|:\\s*\\n\\s*-)",
                java.util.regex.Pattern.MULTILINE);

        // Padrões de Siglas com Definição explícita "Exemplo (EX)"
        private static final java.util.regex.Pattern ACRONYM_DEF_PATTERN = java.util.regex.Pattern.compile(
                "\\b[A-Z][a-zA-Z\\s]+\\s+\\(([A-Z]{2,})\\)",
                java.util.regex.Pattern.MULTILINE);

        public static boolean isCentralConcept(String text) {
            // Se encontrar definição explicita OU lista de componentes OU definição de
            // sigla
            return DEFINITION_PATTERN.matcher(text).find() ||
                    LIST_PATTERN.matcher(text).find() ||
                    ACRONYM_DEF_PATTERN.matcher(text).find();
        }
    }

    // Utilitário simples de Cosseno
    private double cosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length)
            return 0.0;
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
