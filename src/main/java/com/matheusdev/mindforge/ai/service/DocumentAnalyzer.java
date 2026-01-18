package com.matheusdev.mindforge.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Analisador de documentos que detecta tipo, complexidade e características estruturais.
 * Usado para adaptar a estratégia de chunking e retrieval.
 */
@Component
@Slf4j
public class DocumentAnalyzer {

    // Padrões de detecção
    private static final Pattern SECTION_PATTERN = Pattern.compile("\\d+\\.\\d+(?:\\.\\d+)?");
    private static final Pattern TABLE_PATTERN = Pattern.compile("(?s).*\\|.*\\|.*|.*\\d{2,}%.*\\d{2,}%.*");
    private static final Pattern CODE_PATTERN = Pattern.compile("(?s).*(class|function|def|package|import|public|private).*");
    private static final Pattern ACADEMIC_PATTERN = Pattern.compile("(?i).*(abstract|resumo|referências|bibliografia|metodologia).*");

    public enum DocumentType {
        SIMPLE,      // Documentos simples (emails, notas, specs curtas)
        TECHNICAL,   // Documentos técnicos (código, APIs, configs)
        ACADEMIC,    // Documentos acadêmicos (papers, artigos, teses)
        STRUCTURED   // Documentos altamente estruturados (relatórios, manuais)
    }

    public static class DocumentProfile {
        public DocumentType type;
        public int length;
        public boolean hasCode;
        public boolean hasTables;
        public boolean hasSections;
        public int estimatedSections;
        public ComplexityLevel complexity;

        @Override
        public String toString() {
            return String.format("Type=%s, Length=%d, Complexity=%s, Sections=%d, Tables=%s, Code=%s",
                    type, length, complexity, estimatedSections, hasTables, hasCode);
        }
    }

    public enum ComplexityLevel {
        LOW,     // Texto linear simples
        MEDIUM,  // Alguma estruturação
        HIGH     // Altamente estruturado com múltiplas seções/tabelas
    }

    /**
     * Analisa um documento e retorna seu perfil completo.
     */
    public DocumentProfile analyzeDocument(String documentText) {
        DocumentProfile profile = new DocumentProfile();
        profile.length = documentText.length();

        // Detectar características estruturais
        profile.hasCode = CODE_PATTERN.matcher(documentText).find();
        profile.hasTables = TABLE_PATTERN.matcher(documentText).find();
        profile.hasSections = SECTION_PATTERN.matcher(documentText).find();
        profile.estimatedSections = countMatches(documentText, SECTION_PATTERN);

        // Determinar tipo do documento
        profile.type = determineDocumentType(documentText, profile);

        // Calcular complexidade
        profile.complexity = calculateComplexity(profile);

        log.info("📊 Análise de Documento: {}", profile);
        return profile;
    }

    private DocumentType determineDocumentType(String text, DocumentProfile profile) {
        // Acadêmico: tem seções numeradas + padrões acadêmicos
        if (ACADEMIC_PATTERN.matcher(text).find() && profile.hasSections) {
            return DocumentType.ACADEMIC;
        }

        // Técnico: tem código ou muitos termos técnicos
        if (profile.hasCode || containsTechnicalTerms(text)) {
            return DocumentType.TECHNICAL;
        }

        // Estruturado: tem seções + tabelas mas não é acadêmico
        if (profile.hasSections && profile.hasTables) {
            return DocumentType.STRUCTURED;
        }

        // Simples: resto
        return DocumentType.SIMPLE;
    }

    private ComplexityLevel calculateComplexity(DocumentProfile profile) {
        int complexityScore = 0;

        // Pontuação baseada em características
        if (profile.hasSections) complexityScore += 2;
        if (profile.hasTables) complexityScore += 2;
        if (profile.hasCode) complexityScore += 1;
        if (profile.estimatedSections > 10) complexityScore += 2;
        if (profile.length > 50_000) complexityScore += 2;

        if (complexityScore >= 6) return ComplexityLevel.HIGH;
        if (complexityScore >= 3) return ComplexityLevel.MEDIUM;
        return ComplexityLevel.LOW;
    }

    private boolean containsTechnicalTerms(String text) {
        String[] technicalKeywords = {
            "API", "JSON", "XML", "HTTP", "REST", "SQL", "database",
            "algorithm", "function", "method", "class", "interface"
        };
        
        String lowerText = text.toLowerCase();
        int count = 0;
        for (String keyword : technicalKeywords) {
            if (lowerText.contains(keyword.toLowerCase())) count++;
        }
        
        return count >= 3; // Pelo menos 3 termos técnicos
    }

    private int countMatches(String text, Pattern pattern) {
        return (int) pattern.matcher(text).results().count();
    }

    /**
     * Recomenda configurações de chunking baseado no perfil do documento.
     */
    public ChunkingConfig recommendChunkingConfig(DocumentProfile profile) {
        return switch (profile.type) {
            case ACADEMIC -> new ChunkingConfig(
                profile.complexity == ComplexityLevel.HIGH ? 1400 : 1200,
                250,
                "academic"
            );
            case TECHNICAL -> new ChunkingConfig(
                1000,
                200,
                "technical"
            );
            case STRUCTURED -> new ChunkingConfig(
                1200,
                220,
                "structured"
            );
            case SIMPLE -> new ChunkingConfig(
                800,
                150,
                "simple"
            );
        };
    }

    public static class ChunkingConfig {
        public final int chunkSize;
        public final int overlap;
        public final String strategy;

        public ChunkingConfig(int chunkSize, int overlap, String strategy) {
            this.chunkSize = chunkSize;
            this.overlap = overlap;
            this.strategy = strategy;
        }

        @Override
        public String toString() {
            return String.format("ChunkSize=%d, Overlap=%d, Strategy=%s", chunkSize, overlap, strategy);
        }
    }
}
