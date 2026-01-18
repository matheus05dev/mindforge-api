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
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class VectorStoreService {

    private final EmbeddingModel embeddingModel;
    private final DocumentAnalyzer documentAnalyzer;

    private final Map<String, EmbeddingStore<TextSegment>> vectorStores = new ConcurrentHashMap<>();
    private final Map<String, DocumentAnalyzer.DocumentProfile> documentProfiles = new ConcurrentHashMap<>();

    private final Map<String, String> commonTerms = new HashMap<>();
    private final Map<String, String> academicTerms = new HashMap<>();
    private final Map<String, String> technicalTerms = new HashMap<>();

    @PostConstruct
    public void init() {
        loadTerms("term-expansions.properties");
    }

    private void loadTerms(String fileName) {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (input == null) {
                log.error("Unable to find " + fileName);
                return;
            }
            props.load(input);
            for (String key : props.stringPropertyNames()) {
                if (key.startsWith("common.")) {
                    commonTerms.put(key.substring(7), props.getProperty(key));
                } else if (key.startsWith("academic.")) {
                    academicTerms.put(key.substring(9), props.getProperty(key));
                } else if (key.startsWith("technical.")) {
                    technicalTerms.put(key.substring(10), props.getProperty(key));
                }
            }
            log.info("Loaded {} common, {} academic, and {} technical terms.", commonTerms.size(), academicTerms.size(), technicalTerms.size());
        } catch (IOException ex) {
            log.error("Error loading term expansions", ex);
        }
    }

    /**
     * Cria ou retorna um embedding store para um documento específico.
     * ADAPTATIVO: Ajusta chunking e metadados baseado no tipo de documento.
     */
    public EmbeddingStore<TextSegment> getOrCreateVectorStore(String documentId, Document document) {
        EmbeddingStore<TextSegment> existingStore = vectorStores.get(documentId);
        if (existingStore != null) {
            log.info("✅ Vector store já existe para '{}'. Reutilizando...", documentId);
            return existingStore;
        }

        log.info("🔧 Iniciando ingestão RAG ADAPTATIVA para '{}'...", documentId);

        // 1. ANALISAR documento para determinar estratégia
        DocumentAnalyzer.DocumentProfile profile = documentAnalyzer.analyzeDocument(document.text());
        documentProfiles.put(documentId, profile);

        // 2. ENRIQUECER texto com expansões semânticas
        String enrichedText = enrichDocument(document.text(), profile);
        Document enrichedDoc = Document.from(enrichedText, document.metadata());

        // 3. CHUNKING ADAPTATIVO baseado no perfil
        DocumentAnalyzer.ChunkingConfig config = documentAnalyzer.recommendChunkingConfig(profile);
        log.info("📐 Configuração de chunking adaptativo: {}", config);
        
        DocumentSplitter splitter = DocumentSplitters.recursive(config.chunkSize, config.overlap);
        List<TextSegment> rawSegments = splitter.split(enrichedDoc);

        // 4. ENRIQUECER chunks com metadados estruturais
        List<TextSegment> enrichedSegments = enrichSegments(rawSegments, documentId, profile);
        log.info("📦 Documento dividido em {} segmentos enriquecidos.", enrichedSegments.size());

        // 5. GERAR embeddings
        List<Embedding> embeddings = embeddingModel.embedAll(enrichedSegments).content();

        // 6. CRIAR vector store
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.addAll(embeddings, enrichedSegments);
        
        vectorStores.put(documentId, embeddingStore);
        log.info("✅ Vector store criado com sucesso para '{}'.", documentId);

        return embeddingStore;
    }

    /**
     * Enriquece o documento com expansões semânticas (siglas, termos técnicos).
     */
    private String enrichDocument(String text, DocumentAnalyzer.DocumentProfile profile) {
        String enriched = text;

        enriched = expandTerms(enriched, commonTerms);

        if (profile.type == DocumentAnalyzer.DocumentType.ACADEMIC) {
            enriched = expandTerms(enriched, academicTerms);
        }

        if (profile.type == DocumentAnalyzer.DocumentType.TECHNICAL) {
            enriched = expandTerms(enriched, technicalTerms);
        }

        return enriched;
    }

    private String expandTerms(String text, Map<String, String> terms) {
        String expandedText = text;
        for (Map.Entry<String, String> entry : terms.entrySet()) {
            // Use word boundaries to avoid replacing parts of words
            String regex = "\\b" + Pattern.quote(entry.getKey()) + "\\b";
            expandedText = expandedText.replaceAll(regex, Matcher.quoteReplacement(entry.getValue()));
        }
        return expandedText;
    }

    /**
     * Enriquece cada chunk com metadados estruturais inteligentes.
     */
    private List<TextSegment> enrichSegments(
            List<TextSegment> rawSegments, 
            String documentId, 
            DocumentAnalyzer.DocumentProfile profile) {
        
        List<TextSegment> enrichedSegments = new ArrayList<>();
        
        for (int i = 0; i < rawSegments.size(); i++) {
            TextSegment segment = rawSegments.get(i);
            String chunkText = segment.text();
            
            Map<String, String> metadata = new HashMap<>();
            
            // Metadados básicos
            metadata.put("chunk_index", String.valueOf(i));
            metadata.put("document_id", documentId);
            metadata.put("document_type", profile.type.toString());
            
            // Detectar tipo de conteúdo
            String contentType = detectContentType(chunkText, profile);
            metadata.put("content_type", contentType);
            
            // Extrair seção (se houver)
            String section = extractSection(chunkText);
            if (section != null) {
                metadata.put("section", section);
                metadata.put("has_section", "true");
            }
            
            // Detectar tabelas
            if (containsTable(chunkText)) {
                metadata.put("has_table", "true");
                metadata.put("table_type", detectTableType(chunkText));
            }
            
            // Detectar código
            if (containsCode(chunkText)) {
                metadata.put("has_code", "true");
            }
            
            // Detectar listas/enumerações
            if (containsList(chunkText)) {
                metadata.put("has_list", "true");
            }
            
            // Detectar definições/conceitos importantes
            if (containsDefinition(chunkText)) {
                metadata.put("has_definition", "true");
            }
            
            // Para chunks com números/percentuais (importantes!)
            if (containsNumericData(chunkText)) {
                metadata.put("has_numeric_data", "true");
                metadata.put("numeric_values", extractNumericValues(chunkText));
            }
            
            // Criar novo TextSegment enriquecido
            TextSegment enrichedSegment = TextSegment.from(
                chunkText,
                new dev.langchain4j.data.document.Metadata(metadata)
            );
            enrichedSegments.add(enrichedSegment);
        }
        
        return enrichedSegments;
    }

    // ============= MÉTODOS DE DETECÇÃO =============

    private String detectContentType(String text, DocumentAnalyzer.DocumentProfile profile) {
        // Prioridade de detecção
        if (text.matches("(?s).*\\d+\\.\\d+\\.?\\d?.*[A-Z].*")) return "section_header";
        if (containsTable(text)) return "table";
        if (containsCode(text)) return "code";
        if (containsList(text)) return "list";
        if (containsDefinition(text)) return "definition";
        if (text.startsWith("Resumo") || text.startsWith("Abstract")) return "summary";
        if (text.contains("Exemplo") || text.contains("exemplo")) return "example";
        return "prose";
    }

    private String extractSection(String text) {
        Pattern pattern = Pattern.compile("(\\d+\\.\\d+(?:\\.\\d+)?)");
        Matcher matcher = pattern.matcher(text.substring(0, Math.min(150, text.length())));
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private boolean containsTable(String text) {
        return text.matches("(?s).*\\|.*\\|.*") || 
               (text.contains("%") && text.matches("(?s).*\\d{2,3}%.*\\d{2,3}%.*")) ||
               (text.contains("Debt Score") || text.contains("Métrica") || text.contains("Classificação"));
    }

    private String detectTableType(String text) {
        if (text.contains("Debt Score") || text.contains("Score")) return "score_table";
        if (text.contains("%") && text.contains("sprint")) return "allocation_table";
        if (text.contains("Métrica")) return "metrics_table";
        return "generic_table";
    }

    private boolean containsCode(String text) {
        return text.matches("(?s).*(class|function|def|package|import|public|private|void|return).*");
    }

    private boolean containsList(String text) {
        return text.matches("(?s).*[•●■].*") || 
               text.matches("(?s).*\\n\\s*[-*]\\s+.*") ||
               text.matches("(?s).*\\n\\s*\\d+\\.\\s+.*");
    }

    private boolean containsDefinition(String text) {
        return text.matches("(?s).*(é definido como|consiste em|significa|refere-se a|é o processo de).*");
    }

    private boolean containsNumericData(String text) {
        return text.matches("(?s).*\\d+%.*") || 
               text.matches("(?s).*\\d+\\.\\d+.*") ||
               text.matches("(?s).*\\$\\d+.*");
    }

    private String extractNumericValues(String text) {
        Pattern pattern = Pattern.compile("\\d+(?:\\.\\d+)?%?");
        Matcher matcher = pattern.matcher(text);
        List<String> values = new ArrayList<>();
        while (matcher.find() && values.size() < 5) { // Limita a 5 valores
            values.add(matcher.group());
        }
        return String.join(", ", values);
    }

    /**
     * Busca ADAPTATIVA com fallback automático.
     */
    public List<TextSegment> findRelevantSegments(String documentId, String query, int maxResults, double minScore) {
        EmbeddingStore<TextSegment> embeddingStore = vectorStores.get(documentId);
        
        if (embeddingStore == null) {
            log.warn("❌ Vector store não encontrado para '{}'. Retornando lista vazia.", documentId);
            return List.of();
        }

        // Ajustar minScore baseado no perfil do documento
        DocumentAnalyzer.DocumentProfile profile = documentProfiles.get(documentId);
        double adaptiveMinScore = minScore;
        
        if (profile != null && profile.complexity == DocumentAnalyzer.ComplexityLevel.HIGH) {
            adaptiveMinScore = Math.max(0.6, minScore - 0.1); // Mais permissivo para docs complexos
            log.info("🎯 MinScore adaptado para documento complexo: {} → {}", minScore, adaptiveMinScore);
        }

        log.info("🔍 Buscando {} segmentos (score >= {}) para '{}'...", maxResults, adaptiveMinScore, documentId);

        Embedding queryEmbedding = embeddingModel.embed(query).content();
        List<EmbeddingMatch<TextSegment>> relevantMatches = embeddingStore.findRelevant(
            queryEmbedding, maxResults, adaptiveMinScore
        );

        // FALLBACK: Se não encontrou nada, tenta com score mais baixo
        if (relevantMatches.isEmpty() && adaptiveMinScore > 0.5) {
            log.warn("⚠️ Nenhum resultado encontrado. Tentando fallback com score 0.5...");
            relevantMatches = embeddingStore.findRelevant(queryEmbedding, maxResults, 0.5);
        }

        log.info("✅ Encontrados {} segmentos relevantes.", relevantMatches.size());

        return relevantMatches.stream()
                .map(EmbeddingMatch::embedded)
                .toList();
    }

    // Métodos de gerenciamento (mantidos iguais)
    public boolean isDocumentIndexed(String documentId) {
        return vectorStores.containsKey(documentId);
    }

    public void removeVectorStore(String documentId) {
        vectorStores.remove(documentId);
        documentProfiles.remove(documentId);
        log.info("🗑️ Vector store removido para '{}'.", documentId);
    }

    public void clearAllStores() {
        int count = vectorStores.size();
        vectorStores.clear();
        documentProfiles.clear();
        log.info("🗑️ Todos os vector stores removidos (total: {}).", count);
    }
}
