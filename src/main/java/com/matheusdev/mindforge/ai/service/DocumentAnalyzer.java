package com.matheusdev.mindforge.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Analisador de documentos que detecta tipo, complexidade e características
 * estruturais.
 * Usado para adaptar a estratégia de chunking e retrieval.
 */

@Component
@Slf4j
public class DocumentAnalyzer {

    // Padrões de detecção
    private static final Pattern SECTION_PATTERN = Pattern.compile("(\\n|^)\\d+(\\.\\d+)+");

    private static final Pattern TABLE_PATTERN = Pattern.compile("(?s).*\\|.*\\|.*|.*\\d{2,}%.*\\d{2,}%.*");

    private static final Pattern CODE_PATTERN = Pattern
            .compile("(?s).*(class|function|def|package|import|public|private).*");

    private static final Pattern ACADEMIC_PATTERN = Pattern
            .compile("(?i).*(abstract|resumo|referências|bibliografia|metodologia).*");

    // Texto natural com números
    private static final Pattern NUMERIC_TEXT_PATTERN = Pattern.compile(
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
            Pattern.CASE_INSENSITIVE);

    // Ambientes matemáticos LaTeX
    private static final Pattern LATEX_MATH_ENV_PATTERN = Pattern.compile(
            "(\\\\begin\\{equation\\}|\\\\begin\\{align\\}|\\\\begin\\{math\\}|\\$\\$|\\$|\\\\\\[|\\\\\\])",
            Pattern.CASE_INSENSITIVE);

    // Operações matemáticas explícitas
    private static final Pattern LATEX_OPERATION_PATTERN = Pattern.compile(
            "(\\frac\\{|\\times|\\cdot|=|\\+|\\-|\\*)");

    // Listas dinâmicas de termos carregadas do arquivo de propriedades
    private final Set<String> technicalTerms = new HashSet<>();
    private final Set<String> academicTerms = new HashSet<>();
    private final Set<String> securityTerms = new HashSet<>();
    private final Set<String> commonTerms = new HashSet<>();

    // Mapa de definições para Injeção de Conhecimento (Glossário Dinâmico)
    private final Map<String, String> termDefinitions = new HashMap<>();

    public Map<String, String> getTermDefinitions() {
        return Collections.unmodifiableMap(termDefinitions);
    }

    @PostConstruct
    public void loadTermExpansions() {
        try {
            ClassPathResource resource = new ClassPathResource("term-expansions.properties");
            Properties props = PropertiesLoaderUtils.loadProperties(resource);

            for (String key : props.stringPropertyNames()) {
                String value = props.getProperty(key);
                if (value == null || value.trim().isEmpty())
                    continue;

                // Extrai o termo principal (antes dos parênteses, se houver)
                String term = value.split("\\(", 2)[0].trim().toLowerCase();

                // Armazena a definição completa para uso no Prompt
                termDefinitions.put(term, value);

                if (key.startsWith("technical."))
                    technicalTerms.add(term);
                else if (key.startsWith("academic."))
                    academicTerms.add(term);
                else if (key.startsWith("security."))
                    securityTerms.add(term);
                else if (key.startsWith("common."))
                    commonTerms.add(term);
            }

            log.info("📚 Expansões de termos carregadas: {} technical, {} academic, {} security, {} common",
                    technicalTerms.size(), academicTerms.size(), securityTerms.size(), commonTerms.size());

        } catch (IOException e) {
            log.error("❌ Falha ao carregar term-expansions.properties. Usando fallback básico.", e);
            // Fallback básico
            technicalTerms.addAll(Arrays.asList("api", "json", "xml", "http", "rest", "sql", "java", "spring"));
            academicTerms.addAll(Arrays.asList("resumo", "abstract", "metodologia", "conclusão", "referências"));
        }
    }

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

        public Map<String, String> dynamicGlossary = new HashMap<>();
        public Set<String> coreConcepts = new HashSet<>();

        @Override
        public String toString() {
            return String.format(
                    "Type=%s, Complexity=%s, Concepts=%d, Glossary=%d",
                    type, complexity, coreConcepts.size(), dynamicGlossary.size());
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

        // --- LAYER 2: Extração Dinâmica de Entidades ---
        extractDynamicEntities(documentText, profile);

        log.info("📊 Análise de Documento: {}", profile);
        return profile;
    }

    private void extractDynamicEntities(String text, DocumentProfile profile) {
        // 1. Extrair Acrônimos com Definição Explícita: "Unified Modeling Language
        // (UML)"
        // Regex: Palavra(s) iniciando com Maiúscula seguidas de (ACRONIMO)
        Pattern acronymPattern = Pattern.compile("([A-Z][a-zA-Z]+\\s?){1,4}\\s+\\(([A-Z]{2,})\\)");
        java.util.regex.Matcher matcher = acronymPattern.matcher(text);

        while (matcher.find()) {
            String definition = matcher.group(1).trim();
            String acronym = matcher.group(2);

            // Filtros básicos
            if (acronym.length() <= 6 && !commonTerms.contains(acronym.toLowerCase())) {
                profile.dynamicGlossary.put(acronym, definition);
                profile.coreConcepts.add(acronym);
            }
        }

        // 2. Extrair PascalCase repetido (Conceitos Chave)
        // Heurística básica: Termos Capitalizados que aparecem >3 vezes.
        // Implementação simplificada para não onerar CPU.
        // (Pode ser refinado no futuro)
    }

    private DocumentType determineDocumentType(String text, DocumentProfile profile) {
        if ((ACADEMIC_PATTERN.matcher(text).find() || countMatchesFromSet(text, academicTerms) >= 2)
                && profile.hasSections) {
            return DocumentType.ACADEMIC;
        }

        int techScore = countMatchesFromSet(text, technicalTerms);
        int secScore = countMatchesFromSet(text, securityTerms);

        if (profile.hasCode || techScore >= 3 || secScore >= 2) {
            return DocumentType.TECHNICAL;
        }
        if (profile.hasSections && profile.hasTables) {
            return DocumentType.STRUCTURED;
        }
        return DocumentType.SIMPLE;
    }

    private ComplexityLevel calculateComplexity(DocumentProfile profile) {
        int score = 0;
        if (profile.hasSections)
            score += 2;
        if (profile.hasTables)
            score += 2;
        if (profile.hasCode)
            score += 1;
        if (profile.estimatedSections > 10)
            score += 2;
        if (profile.length > 50_000)
            score += 2;

        if (score >= 6)
            return ComplexityLevel.HIGH;
        if (score >= 3)
            return ComplexityLevel.MEDIUM;
        return ComplexityLevel.LOW;
    }

    private int countMatchesFromSet(String text, Set<String> terms) {
        String lower = text.toLowerCase();
        int count = 0;
        // Verifica ocorrência de termos (busca simples de substring para performance em
        // memória)
        // Para maior precisão, poderíamos usar Regex boundary \b, mas isso pode ser
        // caro para muitos termos
        for (String term : terms) {
            if (lower.contains(term)) {
                count++;
            }
        }
        return count;
    }

    private int countMatches(String text, Pattern pattern) {
        return (int) pattern.matcher(text).results().count();
    }

    /**
     * Recomenda configuração de chunking baseada no perfil do documento.
     */

    public ChunkingConfig recommendChunkingConfig(DocumentProfile profile) {
        return switch (profile.type) {
            case ACADEMIC -> new ChunkingConfig(
                    profile.complexity == ComplexityLevel.HIGH ? 1400 : 1200,
                    250,
                    "academic");
            case TECHNICAL -> new ChunkingConfig(1000, 200, "technical");
            case STRUCTURED -> new ChunkingConfig(1200, 220, "structured");
            case SIMPLE -> new ChunkingConfig(800, 150, "simple");
        };
    }

    /**
     * Configuração de chunking.
     */

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
                    chunkSize, overlap, strategy);
        }
    }
}
