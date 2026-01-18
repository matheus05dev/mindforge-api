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
    private static final Pattern SECTION_PATTERN =
            Pattern.compile("(\\n|^)\\d+(\\.\\d+)+");

    private static final Pattern TABLE_PATTERN =
            Pattern.compile("(?s).*\\|.*\\|.*|.*\\d{2,}%.*\\d{2,}%.*");

    private static final Pattern CODE_PATTERN =
            Pattern.compile("(?s).*(class|function|def|package|import|public|private).*");

    private static final Pattern ACADEMIC_PATTERN =
            Pattern.compile("(?i).*(abstract|resumo|referências|bibliografia|metodologia).*");

    // Texto natural com números
    private static final Pattern NUMERIC_TEXT_PATTERN =
            Pattern.compile(
                    "\\b(" +
                            "percent|porcentagem|por\\s+cento|" +
                            "%|\\\\%|" +
                            "tempo|dias|horas|ms|segundos|minutos|semanas|sprint|" +
                            "mttr|sla|score|índice|indice|taxa|nível|nivel|" +
                            "entre|de\\s+\\d+\\s+a\\s+\\d+|entre\\s+\\d+\\s+e\\s+\\d+|" +
                            "maior que|menor que|acima de|abaixo de|" +
                            ">=|<=|>|<|" +
                            "\\\\(leq|geq|approx)|" +
                            "[\\[\\(]\\s*\\d+\\s*,\\s*\\d+\\s*[\\]\\)]|" +
                            "\\d+\\s*-\\s*\\d+" +
                            ")\\b",
                    Pattern.CASE_INSENSITIVE
            );

    // Ambientes matemáticos LaTeX
    private static final Pattern LATEX_MATH_ENV_PATTERN =
            Pattern.compile(
                    "(\\\\begin\\{equation\\}|\\\\begin\\{align\\}|\\\\begin\\{math\\}|\\$\\$|\\$|\\\\\\[|\\\\\\])",
                    Pattern.CASE_INSENSITIVE
            );

    // Operações matemáticas explícitas
    private static final Pattern LATEX_OPERATION_PATTERN =
            Pattern.compile(
                    "(\\\\frac\\{|\\\\times|\\\\cdot|=|\\+|\\-|\\*)"
            );

    public enum DocumentType {
        SIMPLE,
        TECHNICAL,
        ACADEMIC,
        STRUCTURED
    }

    public enum ComplexityLevel {
        LOW,
        MEDIUM,
        HIGH
    }

    public static class DocumentProfile {
        public DocumentType type;
        public int length;
        public boolean hasCode;
        public boolean hasTables;
        public boolean hasSections;
        public int estimatedSections;
        public ComplexityLevel complexity;

        /** Documento contém números explícitos */
        public boolean numericSensitive;

        /** Documento contém matemática inferível (LaTeX, fórmulas, operações) */
        public boolean numericInferenceRisk;

        @Override
        public String toString() {
            return String.format(
                    "Type=%s, Length=%d, Complexity=%s, Sections=%d, Tables=%s, Code=%s, NumericSensitive=%s, NumericInferenceRisk=%s",
                    type, length, complexity, estimatedSections, hasTables, hasCode,
                    numericSensitive, numericInferenceRisk
            );
        }
    }

    public DocumentProfile analyzeDocument(String documentText) {
        DocumentProfile profile = new DocumentProfile();
        profile.length = documentText.length();

        // Estrutura
        profile.hasCode = CODE_PATTERN.matcher(documentText).find();
        profile.hasTables = TABLE_PATTERN.matcher(documentText).find();
        profile.hasSections = SECTION_PATTERN.matcher(documentText).find();
        profile.estimatedSections = countMatches(documentText, SECTION_PATTERN);

        // Numérico
        boolean hasNumericText = NUMERIC_TEXT_PATTERN.matcher(documentText).find();
        boolean hasLatexMath = LATEX_MATH_ENV_PATTERN.matcher(documentText).find();
        boolean hasLatexOps = LATEX_OPERATION_PATTERN.matcher(documentText).find();

        profile.numericSensitive = hasNumericText;
        profile.numericInferenceRisk = hasNumericText && (hasLatexMath || hasLatexOps);

        // Tipo e complexidade
        profile.type = determineDocumentType(documentText, profile);
        profile.complexity = calculateComplexity(profile);

        log.info("📊 Análise de Documento: {}", profile);
        return profile;
    }

    private DocumentType determineDocumentType(String text, DocumentProfile profile) {
        if (ACADEMIC_PATTERN.matcher(text).find() && profile.hasSections) {
            return DocumentType.ACADEMIC;
        }
        if (profile.hasCode || containsTechnicalTerms(text)) {
            return DocumentType.TECHNICAL;
        }
        if (profile.hasSections && profile.hasTables) {
            return DocumentType.STRUCTURED;
        }
        return DocumentType.SIMPLE;
    }

    private ComplexityLevel calculateComplexity(DocumentProfile profile) {
        int score = 0;
        if (profile.hasSections) score += 2;
        if (profile.hasTables) score += 2;
        if (profile.hasCode) score += 1;
        if (profile.estimatedSections > 10) score += 2;
        if (profile.length > 50_000) score += 2;

        if (score >= 6) return ComplexityLevel.HIGH;
        if (score >= 3) return ComplexityLevel.MEDIUM;
        return ComplexityLevel.LOW;
    }

    private boolean containsTechnicalTerms(String text) {
        String[] keywords = {
                "api", "json", "xml", "http", "rest", "sql",
                "algorithm", "function", "method", "class", "interface"
        };
        int count = 0;
        String lower = text.toLowerCase();
        for (String k : keywords) {
            if (lower.contains(k)) count++;
        }
        return count >= 3;
    }

    private int countMatches(String text, Pattern pattern) {
        return (int) pattern.matcher(text).results().count();
    }

    /* ============================
       CHUNKING
       ============================ */

    public ChunkingConfig recommendChunkingConfig(DocumentProfile profile) {
        return switch (profile.type) {
            case ACADEMIC -> new ChunkingConfig(
                    profile.complexity == ComplexityLevel.HIGH ? 1400 : 1200,
                    250,
                    "academic"
            );
            case TECHNICAL -> new ChunkingConfig(1000, 200, "technical");
            case STRUCTURED -> new ChunkingConfig(1200, 220, "structured");
            case SIMPLE -> new ChunkingConfig(800, 150, "simple");
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
            return String.format(
                    "ChunkSize=%d, Overlap=%d, Strategy=%s",
                    chunkSize, overlap, strategy
            );
        }
    }
}
