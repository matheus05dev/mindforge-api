package com.matheusdev.mindforge.ai.service;

import com.matheusdev.mindforge.ai.service.model.Evidence;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class    RAGService {

    private final VectorStoreService vectorStoreService;
    private final DocumentAnalyzer documentAnalyzer;

    /**
     * Processa query com RAG ADAPTATIVO.
     * MinScore e maxResults ajustados automaticamente baseado no documento.
     */
    @org.springframework.cache.annotation.Cacheable(value = com.matheusdev.mindforge.core.config.CacheConfig.RAG_RETRIEVAL_CACHE, key = "{#documentId, #query.trim().toLowerCase(), #maxResults}")
    public List<Evidence> processQueryWithRAG(String documentId, Document document, String query, int maxResults) {
        log.info("🚀 Iniciando RAG ADAPTATIVO para '{}' com query: '{}' (Cache MISS)", documentId, query);

        // Indexar documento (ou reutilizar se já indexado)
        vectorStoreService.getOrCreateVectorStore(documentId, document);

        // Analisar documento para ajustar estratégia de busca
        // Se document é null (vector store já existe), usa profile em cache
        DocumentAnalyzer.DocumentProfile profile = vectorStoreService.getDocumentProfile(documentId);
        if (profile == null && document != null) {
            profile = documentAnalyzer.analyzeDocument(document.text());
        }
        if (profile == null) {
            log.warn("⚠️ Profile não encontrado para '{}'. Usando configuração padrão.", documentId);
            profile = new DocumentAnalyzer.DocumentProfile();
            profile.type = DocumentAnalyzer.DocumentType.SIMPLE;
            profile.length = 0;
            profile.complexity = DocumentAnalyzer.ComplexityLevel.MEDIUM;
            profile.estimatedSections = 0;
            profile.hasCode = false;
            profile.hasTables = false;
            profile.hasSections = false;
            profile.numericSensitive = false;
            profile.numericInferenceRisk = false;
        }

        // Calcular minScore adaptativo
        double minScore = calculateAdaptiveMinScore(profile, query);

        // Ajustar maxResults baseado na complexidade
        int adaptiveMaxResults = calculateAdaptiveMaxResults(profile, maxResults);

        log.info("📊 Busca adaptativa: maxResults={}, minScore={}, docType={}",
                adaptiveMaxResults, minScore, profile.type);

        // --- DIVERSITY RERANKING & FILTERING ---
        // Buscamos 3x mais candidatos para poder filtrar duplicatas e cabeçalhos
        // inúteis
        int fetchCandidates = Math.min(60, adaptiveMaxResults * 3);

        // Buscar segmentos (Pool de Candidatos)
        List<EmbeddingMatch<TextSegment>> rawMatches = vectorStoreService.findRelevantSegments(
                documentId, query, fetchCandidates, minScore);

        List<EmbeddingMatch<TextSegment>> relevantMatches = new ArrayList<>();

        // Dedup logic
        for (EmbeddingMatch<TextSegment> match : rawMatches) {
            if (relevantMatches.size() >= adaptiveMaxResults)
                break;

            TextSegment segment = match.embedded();
            Map<String, String> meta = segment.metadata().asMap();
            String contentType = meta.getOrDefault("content_type", "prose");

            // 1. Filtrar Section Headers sem conteúdo (Miopia)
            // Se for header e não tiver dados, exige score muito alto ou descarta
            if ("section_header".equals(contentType)) {
                boolean hasData = meta.containsKey("numeric_values") || meta.containsKey("has_list");
                // Se é um header puro (só título), penalizamos
                if (!hasData) {
                    // Se o score não for excelente (>0.92), ignora para dar espaço a conteúdo
                    if (match.score() < 0.92) {
                        log.debug("📉 Filtrando Header irrelevante: '{}' (Score: {})",
                                segment.text().substring(0, Math.min(50, segment.text().length())), match.score());
                        continue;
                    }
                }
            }

            // 2. Filtrar Redundância Semântica (Flood)
            boolean isRedundant = false;
            for (EmbeddingMatch<TextSegment> selected : relevantMatches) {
                // EmbeddingMatch do langchain4j usually has embedding if store provided it.
                // Assuming InMemoryEmbeddingStore which does.
                if (match.embedding() != null && selected.embedding() != null) {
                    double similarity = cosineSimilarity(match.embedding().vector(), selected.embedding().vector());
                    if (similarity > 0.88) { // Se for 88% similar a algo já escolhido
                        isRedundant = true;
                        log.debug("🔄 Filtrando Redundância: '{}' similar a anterior (Sim: {})",
                                segment.text().substring(0, Math.min(30, segment.text().length())), similarity);
                        break;
                    }
                }
            }

            if (!isRedundant) {
                relevantMatches.add(match);
            }
        }

        if (relevantMatches.isEmpty()) {
            log.warn("⚠️ Nenhum segmento relevante encontrado para query '{}'.", query);
        } else {
            log.info("✅ RAG processado. {} segmentos relevantes selecionados (de {} candidatos).",
                    relevantMatches.size(), rawMatches.size());

            // --- LOG DE DIAGNÓSTICO DE CHUNKS ---
            log.info("--- 🔍 CONTEÚDO DOS CHUNKS RECUPERADOS ---");
            for (int i = 0; i < relevantMatches.size(); i++) {
                EmbeddingMatch<TextSegment> match = relevantMatches.get(i);
                String text = match.embedded().text();
                String preview = text.length() > 300 ? text.substring(0, 300) + "..." : text;

                // Se parecer uma tabela (Markdown), logar tudo para verificar integridade
                if (text.contains("|") && text.contains("---")) {
                    preview = "[TABELA DETECTADA] \n" + text;
                }

                log.info("Chunk #{}: Score={:.4f} | Metadata={} \nConteúdo: {}",
                        i + 1,
                        match.score(),
                        match.embedded().metadata().asMap(),
                        preview);
            }
            log.info("--- 🏁 FIM DOS CHUNKS ---");
            // -------------------------------------
        }

        // Calcular limite seguro de budget
        // Groq limit: 6000 TPM. Vamos usar ~5500 tokens de margem segura.
        // 1 token ~= 4 chars (pior caso em PT-BR/Código pode ser 3 chars)
        // Limite seguro em caracteres: 22.000 chars (~5500 tokens)
        final int MAX_TOTAL_CHARS = 22_000;
        int currentTotalChars = 0;

        List<Evidence> evidenceList = new ArrayList<>();

        for (EmbeddingMatch<TextSegment> match : relevantMatches) {
            TextSegment segment = match.embedded();

            // 1. Otimização de Payload (Compressão de Whitespace)
            // Remove quebras de linha múltiplas e espaços extras para economizar tokens
            String cleanText = segment.text()
                    .replaceAll("\\r\\n", "\n") // Normalizar quebras Windows
                    .replaceAll("\\n{2,}", "\n") // Reduzir quebras múltiplas para única
                    .replaceAll("[ \\t]+", " ") // Compactar espaços horinzontais
                    .trim();

            // 2. Verificação de Budget (Truncagem Dinâmica)
            if (currentTotalChars + cleanText.length() > MAX_TOTAL_CHARS) {
                int remainingChars = MAX_TOTAL_CHARS - currentTotalChars;
                if (remainingChars > 100) { // Só adiciona se valer a pena (mais de 100 chars)
                    String truncatedText = cleanText.substring(0, remainingChars) + "... [TRUNCADO POR BUDGET]";
                    evidenceList.add(new Evidence(
                            documentId,
                            segment.metadata().getString("section"),
                            segment.metadata().getInteger("page"),
                            segment.metadata().getString("content_type"),
                            truncatedText,
                            match.score(),
                            convertMetadata(segment.metadata())));
                    log.warn("⚠️ RAG Payload limit atingido! Truncando último chunk (Total Chars: {}/{})",
                            currentTotalChars + remainingChars, MAX_TOTAL_CHARS);
                } else {
                    log.warn("⚠️ RAG Payload limit atingido! Ignorando chunks restantes (Total Chars: {}/{})",
                            currentTotalChars, MAX_TOTAL_CHARS);
                }
                break; // Pare de adicionar chunks
            }

            evidenceList.add(new Evidence(
                    documentId,
                    segment.metadata().getString("section"),
                    segment.metadata().getInteger("page"),
                    segment.metadata().getString("content_type"),
                    cleanText,
                    match.score(),
                    convertMetadata(segment.metadata()))); // Passando metadados completos

            currentTotalChars += cleanText.length();
        }

        return evidenceList;
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
     * Calcula maxResults adaptativo baseado no perfil, otimizado para economia de
     * tokens.
     */
    private int calculateAdaptiveMaxResults(DocumentAnalyzer.DocumentProfile profile, int requestedMax) {
        int adaptive = requestedMax;

        // Para documentos complexos, precisamos de mais contexto
        if (profile.complexity == DocumentAnalyzer.ComplexityLevel.HIGH) {
            adaptive = Math.max(requestedMax, 25); // Aumentado para 25 (era 8)
        }

        // Para documentos muito longos (> 50k chars), garantimos cobertura
        if (profile.length > 50_000) {
            adaptive = Math.max(adaptive, 30); // Aumentado para 30 (era 10)
        }

        // Teto mais generoso para permitir que o filtro de diversidade trabalhe
        // Groq/Llama suportam janela de contexto maior, então 40 chunks (~8k chars) é
        // seguro
        return Math.min(adaptive, 40); // Aumentado para 40 (era 12)
    }

    public void indexDocument(String documentId, Document document) {
        log.info("📥 Indexando documento '{}'...", documentId);
        vectorStoreService.getOrCreateVectorStore(documentId, document);
        log.info("✅ Documento '{}' indexado.", documentId);
    }

    public List<Evidence> queryIndexedDocument(String documentId, String query, int maxResults) {
        List<EmbeddingMatch<TextSegment>> relevantMatches = vectorStoreService.findRelevantSegments(documentId, query,
                maxResults, 0.7);
        return relevantMatches.stream()
                .map(match -> {
                    TextSegment segment = match.embedded();
                    return new Evidence(
                            documentId,
                            segment.metadata().getString("section"),
                            segment.metadata().getInteger("page"),
                            segment.metadata().getString("content_type"),
                            segment.text(),
                            match.score(),
                            convertMetadata(segment.metadata()));
                })
                .collect(Collectors.toList());
    }

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

    @SuppressWarnings("deprecation")
    private java.util.Map<String, Object> convertMetadata(dev.langchain4j.data.document.Metadata metadata) {
        return new java.util.HashMap<>(metadata.asMap());
    }
}
