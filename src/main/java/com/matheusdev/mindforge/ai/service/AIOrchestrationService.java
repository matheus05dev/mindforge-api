package com.matheusdev.mindforge.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusdev.mindforge.ai.chat.model.ChatMessage;
import com.matheusdev.mindforge.ai.chat.model.ChatSession;
import com.matheusdev.mindforge.ai.dto.ChatRequest;
import com.matheusdev.mindforge.ai.dto.PromptPair;
import com.matheusdev.mindforge.ai.memory.model.UserProfileAI;
import com.matheusdev.mindforge.ai.memory.service.MemoryService;
import com.matheusdev.mindforge.ai.provider.AIProvider;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.ai.service.model.Answer;
import com.matheusdev.mindforge.ai.service.model.AuditedAnswer;
import com.matheusdev.mindforge.ai.service.model.Evidence;
import com.matheusdev.mindforge.ai.service.model.EvidenceRef;
import com.matheusdev.mindforge.ai.service.model.InteractionType;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIOrchestrationService {

    private final Map<String, AIProvider> aiProviders;
    private final MemoryService memoryService;
    private final PromptBuilderService promptBuilderService;
    private final ChatService chatService;
    private final SmartRouterService smartRouterService;
    private final RAGService ragService;
    private final DocumentAnalyzer documentAnalyzer;
    private final ObjectMapper objectMapper;
    private final PromptCacheService promptCacheService;
    private final VectorStoreService vectorStoreService;
    private final com.matheusdev.mindforge.knowledgeltem.repository.KnowledgeItemRepository knowledgeItemRepository;
    private final com.matheusdev.mindforge.ai.chat.repository.ChatSessionRepository chatSessionRepository;

    private static final String DEFAULT_PROVIDER = "ollamaProvider";
    private static final String FALLBACK_PROVIDER = "groqProvider";
    private static final int OLLAMA_CHAR_LIMIT = 5000;
    private static final long MAX_VALID_SESSION_ID = 1_000_000_000L;
    private final Semaphore semaphore = new Semaphore(1);

    public CompletableFuture<AIProviderResponse> executeInternalAnalysis(String prompt, String systemMessage) {
        log.info(">>> [ORCHESTRATOR] Executando análise interna (headless)...");

        // FIX: Revertido para Default (Ollama) para economizar tokens do Groq.
        // O usuário prefere processamento local para tarefas de background.
        String providerName = DEFAULT_PROVIDER;

        AIProvider selectedProvider = getProvider(providerName);

        AIProviderRequest request = new AIProviderRequest(prompt, systemMessage, null, providerName);
        return executeAndLogTask(request, selectedProvider, "internal-analysis");
    }

    private ChatSession ensureSession(Long chatId) {
        if (chatId == null || chatId > MAX_VALID_SESSION_ID) {
            if (chatId != null) {
                log.warn("ID de sessão inválido ou muito grande detectado: {}. Criando sessão de emergência.", chatId);
            } else {
                log.warn("ID de sessão nulo detectado. Criando sessão de emergência.");
            }
            return chatService.createEmergencySession();
        }
        return chatService.getSession(chatId)
                .orElseGet(() -> {
                    log.warn("Sessão com ID {} não encontrada. Criando sessão de emergência.", chatId);
                    return chatService.createEmergencySession();
                });
    }

    public CompletableFuture<AIProviderResponse> handleChatInteraction(ChatRequest chatRequest) {
        ChatSession session = ensureSession(chatRequest.chatId());

        log.info(">>> [ORCHESTRATOR] Iniciando interação de chat para sessão: {}", session.getId());

        String userPrompt = (chatRequest.prompt() == null || chatRequest.prompt().isBlank())
                ? "(Interação iniciada sem prompt)"
                : chatRequest.prompt();

        if (chatRequest.prompt() == null || chatRequest.prompt().isBlank()) {
            log.warn("Prompt nulo ou vazio recebido para a sessão {}. Usando conteúdo padrão.", session.getId());
        }

        ChatMessage userMessage = chatService.saveMessage(session, "user", userPrompt);
        log.info("Mensagem do usuário salva no banco: {}", userMessage.getId());

        // FIX: Recuperação de Contexto
        // Se a sessão perdeu o ID do documento (vácuo de contexto), mas o request
        // trouxe o ID,
        // forçamos a atualização da sessão antes de continuar.
        if (!StringUtils.hasText(session.getDocumentId()) && StringUtils.hasText(chatRequest.documentId())) {
            log.warn(
                    "⚠️ CORREÇÃO DE CONTEXTO: Sessão {} estava sem documentId, mas request informou '{}'. Atualizando...",
                    session.getId(), chatRequest.documentId());
            session.setDocumentId(chatRequest.documentId());
            chatService.updateSession(session);
        }

        if (StringUtils.hasText(session.getDocumentId())) {
            log.info("Sessão vinculada ao documento: '{}'. Ativando fluxo RAG.", session.getDocumentId());

            final Long userId = 1L; // Assumindo um usuário fixo por enquanto
            UserProfileAI userProfile = memoryService.getProfile(userId);

            String providerName = getProviderName(chatRequest.provider());
            AIProvider selectedProvider = getProvider(providerName);

            // --- LAYER 3: Discovery & Pre-Analysis ---
            DocumentAnalyzer.DocumentProfile docProfile = vectorStoreService
                    .getDocumentProfile(session.getDocumentId());
            String expandedQuery = expandQueryWithDynamicTerms(userPrompt, docProfile);
            if (!expandedQuery.equals(userPrompt)) {
                log.info("🔍 Query Expandida (Layer 3): '{}' -> '{}'", userPrompt, expandedQuery);
            }

            List<Evidence> evidences = ragService.processQueryWithRAG(session.getDocumentId(), null, expandedQuery, 8);

            // GERA PROMPT COM CONTEXTO DO USUÁRIO
            PromptPair userAwarePrompts = promptBuilderService.buildGenericPrompt(userPrompt, userProfile,
                    Optional.empty(), Optional.empty());
            String finalSystemPrompt = userAwarePrompts.systemPrompt();

            // Se o request original tinha instrução de sistema (ex: frontend passou algo
            // específico), adicionamos.
            if (chatRequest.systemMessage() != null && !chatRequest.systemMessage().isBlank()) {
                finalSystemPrompt += "\n\n" + chatRequest.systemMessage();
            }

            return processWithRAG(session.getDocumentId(), null, userPrompt,
                    new PromptPair(finalSystemPrompt, userPrompt), selectedProvider, providerName, evidences,
                    docProfile)
                    .thenCompose(response -> saveResponseAndUpdateProfile(response, session, userMessage, userId,
                            InteractionType.RAG_ANALYSIS, evidences));
        } else {
            log.info("Sessão de chat puro. Sem contexto de documento.");

            final Long userId = 1L;
            UserProfileAI userProfile = memoryService.getProfile(userId);

            String providerName = getProviderName(chatRequest.provider());
            AIProvider selectedProvider = getProvider(providerName);

            // GERA PROMPT COM CONTEXTO DO USUÁRIO
            PromptPair userAwarePrompts = promptBuilderService.buildGenericPrompt(userPrompt, userProfile,
                    Optional.empty(), Optional.empty());

            String finalSystemPrompt = enrichSystemPromptWithGlossary(userAwarePrompts.systemPrompt(), userPrompt,
                    null);

            if (chatRequest.systemMessage() != null && !chatRequest.systemMessage().isBlank()) {
                finalSystemPrompt += "\n\n" + chatRequest.systemMessage();
            }

            AIProviderRequest request = new AIProviderRequest(
                    userPrompt,
                    finalSystemPrompt,
                    chatRequest.model(),
                    providerName);

            return executeAndLogTask(request, selectedProvider, "chat interaction")
                    .thenCompose(response -> saveResponseAndUpdateProfile(
                            response,
                            session,
                            userMessage,
                            1L, // userId fixo
                            InteractionType.CHAT,
                            null));
        }
    }

    public CompletableFuture<AIProviderResponse> handleFileAnalysis(String userPrompt, String providerName,
            MultipartFile file) throws IOException {
        log.info(">>> [ORCHESTRATOR] Iniciando análise de arquivo: {}", file.getOriginalFilename());

        final Long userId = 1L;
        UserProfileAI userProfile = memoryService.getProfile(userId);
        log.info("Perfil do usuário carregado: {}", userProfile.getSummary());

        List<Document> langchainDocuments = new ArrayList<>();
        boolean isImage = file.getContentType() != null && file.getContentType().startsWith("image/");

        if (!isImage) {
            TikaDocumentReader documentReader = new TikaDocumentReader(file.getResource());
            List<org.springframework.ai.document.Document> springDocuments = documentReader.get();
            langchainDocuments = springDocuments.stream()
                    .map(springDoc -> Document.from(springDoc.getContent(), new Metadata(springDoc.getMetadata())))
                    .collect(Collectors.toList());
            log.info("Documentos extraídos e convertidos para o formato LangChain4j. Total: {}.",
                    langchainDocuments.size());
        }

        String selectedProviderName = providerName;

        if (providerName == null || providerName.isBlank() || "null".equalsIgnoreCase(providerName)) {
            if (isImage) {
                selectedProviderName = "ollamaProvider";
                log.info("🤖 Smart Routing: Imagem detectada -> Usando Ollama (Multimodal/Qwen).");
            } else {
                long totalChars = langchainDocuments.stream().mapToLong(doc -> doc.text().length()).sum();
                if (totalChars > OLLAMA_CHAR_LIMIT) {
                    selectedProviderName = FALLBACK_PROVIDER;
                    log.info("🤖 Smart Routing: Texto GRANDE ({} chars > {}) -> Usando Groq (Cloud/Rápido).",
                            totalChars, OLLAMA_CHAR_LIMIT);
                } else {
                    selectedProviderName = DEFAULT_PROVIDER;
                    log.info("🤖 Smart Routing: Texto PEQUENO ({} chars <= {}) -> Usando Ollama (Local/Privado).",
                            totalChars, OLLAMA_CHAR_LIMIT);
                }
            }
        } else {
            selectedProviderName = getProviderName(providerName);
            log.info("🔒 Provedor forçado pelo usuário: {}", selectedProviderName);
        }

        AIProvider selectedProvider = getProvider(selectedProviderName);

        ChatSession session = chatService.createDocumentAnalysisSession(file.getOriginalFilename(), userPrompt);
        log.info("Sessão de chat criada: {}", session.getId());

        String userMessageContent = String.format("Arquivo: %s\n\nPrompt: %s", file.getOriginalFilename(), userPrompt);
        ChatMessage userMessage = chatService.saveMessage(session, "user", userMessageContent);
        log.info("Mensagem do usuário salva no banco: {}", userMessage.getId());

        PromptPair basePrompts = promptBuilderService.buildGenericPrompt(userPrompt, userProfile, Optional.empty(),
                Optional.empty());
        log.info("Prompt de sistema base gerado.");

        // Para File Analysis também tentamos pegar o profile se já existir (gerado no
        // processamento inicial)
        // Mas geralmente o profile é gerado DENTRO da análise se for RAG.
        // Aqui é fluxo inicial, a análise acontece depois.
        String finalSystemPrompt = enrichSystemPromptWithGlossary(basePrompts.systemPrompt(), userPrompt, null);

        if (isImage) {
            log.info("Iniciando fluxo de análise de imagem.");
            AIProviderRequest request = new AIProviderRequest(
                    userPrompt,
                    finalSystemPrompt,
                    null,
                    selectedProviderName,
                    true,
                    file.getBytes(),
                    file.getContentType(),
                    null,
                    null,
                    null);
            return executeAndLogTask(request, selectedProvider, "análise de imagem")
                    .thenCompose(response -> saveResponseAndUpdateProfile(response, session, userMessage, userId,
                            InteractionType.DOCUMENT_ANALYSIS, null));

        } else {
            String documentId = file.getOriginalFilename() != null ? file.getOriginalFilename()
                    : "document_" + System.currentTimeMillis();
            Document mainDocument = langchainDocuments.get(0);

            SmartRouterService.ProcessingStrategy strategy = smartRouterService
                    .decideStrategy(mainDocument.text().length());

            return switch (strategy) {
                case ONE_SHOT ->
                    processOneShot(mainDocument.text(), new PromptPair(finalSystemPrompt, basePrompts.userPrompt()),
                            selectedProvider, selectedProviderName, userPrompt)
                            .thenCompose(response -> saveResponseAndUpdateProfile(response, session, userMessage,
                                    userId, InteractionType.DOCUMENT_ANALYSIS, null));

                case MAP_REDUCE -> {
                    log.info("Iniciando fluxo Map-Reduce para texto.");
                    boolean isLocalProvider = selectedProviderName.equalsIgnoreCase("ollamaProvider");

                    CompletableFuture<AIProviderResponse> result;
                    if (isLocalProvider) {
                        result = processChunksSequentially(langchainDocuments,
                                new PromptPair(finalSystemPrompt, basePrompts.userPrompt()), selectedProvider,
                                selectedProviderName, userPrompt);
                    } else {
                        result = processChunksWithRateLimit(langchainDocuments,
                                new PromptPair(finalSystemPrompt, basePrompts.userPrompt()), selectedProvider,
                                selectedProviderName, userPrompt);
                    }
                    yield result.thenCompose(response -> saveResponseAndUpdateProfile(response, session, userMessage,
                            userId, InteractionType.DOCUMENT_ANALYSIS, null));
                }

                case RAG -> {
                    List<Evidence> evidences = ragService.processQueryWithRAG(documentId, mainDocument, userPrompt, 8);

                    yield processWithRAG(documentId, mainDocument, userPrompt,
                            new PromptPair(finalSystemPrompt, basePrompts.userPrompt()), selectedProvider,
                            selectedProviderName, evidences, null)
                            .thenCompose(response -> saveResponseAndUpdateProfile(response, session, userMessage,
                                    userId, InteractionType.RAG_ANALYSIS, evidences));
                }

            };
        }
    }

    private CompletableFuture<AIProviderResponse> processChunksSequentially(
            List<Document> chunks,
            PromptPair basePrompts,
            AIProvider provider,
            String providerName,
            String userPrompt) {

        log.info("🔄 Processamento SEQUENCIAL iniciado (Otimizado para Ollama)");

        return CompletableFuture.supplyAsync(() -> {
            List<String> partialResults = new ArrayList<>();

            for (int i = 0; i < chunks.size(); i++) {
                Document chunk = chunks.get(i);
                log.info("📄 Processando chunk {}/{} sequencialmente...", i + 1, chunks.size());

                String mapPrompt = String.format(
                        "Analise e resuma esta parte do documento focado em extrair insights, pontos chave e conclusões parciais: \n\n---\n%s\n\n---",
                        chunk.text());

                AIProviderRequest mapRequest = new AIProviderRequest(mapPrompt, basePrompts.systemPrompt(), null,
                        providerName);

                try {
                    AIProviderResponse response = provider.executeTask(mapRequest).get();
                    partialResults.add(response.getContent());
                    log.info("✅ Chunk {}/{} processado com sucesso.", i + 1, chunks.size());
                } catch (InterruptedException | ExecutionException e) {
                    log.error("❌ Erro ao processar chunk {}/{}: {}", i + 1, chunks.size(), e.getMessage());
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            return reduceResults(partialResults, basePrompts, provider, providerName, userPrompt);
        });
    }

    private CompletableFuture<AIProviderResponse> processChunksWithRateLimit(
            List<Document> chunks,
            PromptPair basePrompts,
            AIProvider provider,
            String providerName,
            String userPrompt) {

        log.info("🚦 Iniciado 'Throughput Shaping' para Groq. Processamento sequencial com cooldown.");

        return CompletableFuture.supplyAsync(() -> {
            List<String> partialResults = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                Document chunk = chunks.get(i);
                try {
                    semaphore.acquire();
                    log.info("📄 Processando chunk {}/{} ({} tokens)...", i + 1, chunks.size(), 800);

                    String mapPrompt = String.format(
                            "Analise e resuma esta parte do documento focado em extrair insights, pontos chave e conclusões parciais: \n\n---\n%s\n\n---",
                            chunk.text());
                    AIProviderRequest mapRequest = new AIProviderRequest(mapPrompt, basePrompts.systemPrompt(), null,
                            providerName);

                    AIProviderResponse response = provider.executeTask(mapRequest).get();
                    partialResults.add(response.getContent());
                    log.info("✅ Chunk {}/{} processado. Aguardando cooldown...", i + 1, chunks.size());

                    if (i < chunks.size() - 1) {
                        log.info("⏳ Cooldown de 25 segundos para respeitar o limite de TPM do Groq.");
                        Thread.sleep(25000);
                    }

                } catch (InterruptedException | ExecutionException e) {
                    log.error("❌ Erro ao processar chunk {}/{}: {}", i + 1, chunks.size(), e.getMessage());
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    break;
                } finally {
                    semaphore.release();
                }
            }
            log.info("✅ Todos os chunks processados. Iniciando redução final.");
            return reduceResults(partialResults, basePrompts, provider, providerName, userPrompt);
        });
    }

    private AIProviderResponse reduceResults(List<String> partialResults, PromptPair basePrompts, AIProvider provider,
            String providerName, String userPrompt) {
        if (partialResults.isEmpty()) {
            log.warn("Nenhum resultado parcial foi gerado. A redução foi abortada.");
            return new AIProviderResponse("Não foi possível gerar um resumo pois a análise inicial falhou.",
                    null, "Análise inicial falhou.", null, InteractionType.SYSTEM);
        }
        String combinedPartials = String.join("\n\n---\n\n", partialResults);
        log.info("Tamanho do texto combinado para redução: {} caracteres.", combinedPartials.length());

        String reducePrompt = String.format(
                "Junte as análises parciais a seguir em um relatório final coeso e bem estruturado, respondendo à solicitação original do usuário. Solicitação do usuário: '%s'.\n\n--- ANÁLISES PARCIAIS ---\n%s",
                userPrompt, combinedPartials);

        AIProviderRequest reduceRequest = new AIProviderRequest(reducePrompt, basePrompts.systemPrompt(), null,
                providerName);

        try {
            return executeAndLogTask(reduceRequest, provider, "análise final (Reduce)").get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erro na redução final", e);
        }
    }

    private CompletableFuture<AIProviderResponse> processOneShot(
            String documentContent,
            PromptPair basePrompts,
            AIProvider provider,
            String providerName,
            String userPrompt) {

        log.info("🚀 Processamento ONE-SHOT: Enviando documento completo diretamente para a IA.");

        String fullPrompt = String.format(
                "%s\n\n--- CONTEÚDO DO DOCUMENTO ---\n%s\n\n--- FIM DO DOCUMENTO ---\n\nCom base no conteúdo acima, %s",
                userPrompt, documentContent, userPrompt);

        AIProviderRequest request = new AIProviderRequest(
                fullPrompt,
                basePrompts.systemPrompt(),
                null,
                providerName);

        return executeAndLogTask(request, provider, "análise one-shot");
    }

    private CompletableFuture<AIProviderResponse> processWithRAG(
            String documentId,
            Document document,
            String userPrompt,
            PromptPair basePrompts,
            AIProvider provider,
            String providerName,
            List<Evidence> evidences,
            DocumentAnalyzer.DocumentProfile docProfile) {

        log.info("🔍 Processamento RAG: Iniciando nova estratégia de 2 etapas.");

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (evidences.isEmpty()) {
                    log.warn("Nenhum segmento relevante encontrado. Retornando resposta padrão.");
                    return new AIProviderResponse(getNotFoundJson(), null, "RAG fallback", null,
                            InteractionType.RAG_ANALYSIS);
                }

                String evidenceText = formatEvidences(evidences);

                // Usando o novo PromptBuilder "Auditor" para garantir guardrails
                PromptPair ragPrompts = promptBuilderService.buildAuditorPrompt(userPrompt, evidenceText);
                String extractionPrompt = ragPrompts.userPrompt();

                String baseSystemPrompt = ragPrompts.systemPrompt();
                // Ainda enriquecemos o system prompt do auditor com o glossário dinâmico
                String systemPrompt = enrichSystemPromptWithGlossary(baseSystemPrompt, userPrompt, docProfile);

                AIProviderRequest extractionRequest = new AIProviderRequest(
                        extractionPrompt,
                        systemPrompt,
                        null,
                        providerName,
                        false,
                        null,
                        null,
                        null,
                        null,
                        0.2 // Temperatura baixa para precisão
                );

                log.info("📤 Etapa 1: Enviando prompt de extração para '{}'.", providerName);
                AIProviderResponse extractionResponse = executeAndLogTask(extractionRequest, provider, "Extração RAG")
                        .get();
                String extractedContent = extractionResponse.getContent();

                log.info("✅ Etapa 1: Resposta de extração recebida.");

                AuditedAnswer finalAnswer;
                // FIX: A lógica anterior descartava respostas parciais se contivesse a frase.
                // Agora verificamos se a resposta é, em essência, APENAS a negativa.
                boolean isEssentiallyEmpty = extractedContent == null || extractedContent.isBlank();
                boolean isStrictlyNotFound = extractedContent != null &&
                        (extractedContent.trim().equalsIgnoreCase("INFORMAÇÃO NÃO ENCONTRADA") ||
                                extractedContent.trim().equalsIgnoreCase("INFORMAÇÃO NÃO ENCONTRADA."));

                if (isEssentiallyEmpty || isStrictlyNotFound) {
                    log.warn("IA não encontrou a informação nas evidências (Resposta explícita de 'Não Encontrado').");
                    finalAnswer = new AuditedAnswer(
                            new Answer("A informação solicitada não foi encontrada no documento.",
                                    "A informação solicitada não foi encontrada no documento."),
                            Collections.emptyList());
                } else {
                    log.info("✅ Etapa 2: Construindo resposta auditada com o conteúdo extraído.");
                    List<EvidenceRef> evidenceRefs = IntStream.range(0, evidences.size())
                            .mapToObj(i -> new EvidenceRef(i + 1, evidences.get(i).excerpt()))
                            .collect(Collectors.toList());

                    finalAnswer = new AuditedAnswer(
                            new Answer(extractedContent, extractedContent), // Usando o mesmo para markdown e plainText
                                                                            // por simplicidade
                            evidenceRefs);
                }

                String finalJson = objectMapper.writeValueAsString(finalAnswer);
                return new AIProviderResponse(finalJson, null, "RAG - 2 Etapas", null, InteractionType.RAG_ANALYSIS);

            } catch (Exception e) {
                log.error("Erro ao processar documento com RAG (2 etapas): {}", e.getMessage(), e);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                return new AIProviderResponse(getFailureJson(), null, "RAG Error", null, InteractionType.RAG_ANALYSIS);
            }
        });
    }

    private static final int MAX_EVIDENCE_CHARS = 12000; // ~3000 tokens, otimizado para limite de 6k TPM

    private String formatEvidences(List<Evidence> evidences) {
        StringBuilder sb = new StringBuilder();
        int currentLength = 0;
        List<Evidence> selectedEvidences = new ArrayList<>();

        // 1. Prioridade: Tabelas e Métricas (Evita alucinação de dados críticos)
        List<Evidence> priorityEvidences = evidences.stream()
                .filter(this::isTableOrMetrics)
                .toList();

        // 2. Standard: Texto prosaico comum
        List<Evidence> standardEvidences = evidences.stream()
                .filter(e -> !isTableOrMetrics(e))
                .toList();

        // Strategy: Fill with Priority first, then Standard, then Sort by original
        // index.

        // Add Priority Evidences
        for (Evidence e : priorityEvidences) {
            String formatted = formatEvidenceItem(e, -1);
            if (currentLength + formatted.length() <= MAX_EVIDENCE_CHARS) {
                selectedEvidences.add(e);
                currentLength += formatted.length();
            } else {
                log.warn("⚠️ Tabela crítica ignorada por falta de espaço no limite de {} chars!", MAX_EVIDENCE_CHARS);
            }
        }

        // Add Standard Evidences (until limit)
        for (Evidence e : standardEvidences) {
            String formatted = formatEvidenceItem(e, -1);
            if (currentLength + formatted.length() <= MAX_EVIDENCE_CHARS) {
                selectedEvidences.add(e);
                currentLength += formatted.length();
            }
        }

        // Reorder list to original flow (Intersection of Original & Selected)
        List<Evidence> finalSelection = evidences.stream()
                .filter(selectedEvidences::contains)
                .toList();

        log.info("📊 RAG Payload: {}/{} chars. {} evidências selecionadas de {} ({} Prioritárias).",
                currentLength, MAX_EVIDENCE_CHARS, finalSelection.size(), evidences.size(), priorityEvidences.size());

        for (int i = 0; i < finalSelection.size(); i++) {
            sb.append(formatEvidenceItem(finalSelection.get(i), i + 1)).append("\n");
        }

        return sb.toString();
    }

    private boolean isTableOrMetrics(Evidence e) {
        String type = e.contentType() != null ? e.contentType().toLowerCase() : "";
        String excerpt = e.excerpt().toLowerCase();
        return type.contains("table") || type.contains("metric") || excerpt.contains("table_type=")
                || excerpt.contains("has_table=true");
    }

    private String expandQueryWithDynamicTerms(String query, DocumentAnalyzer.DocumentProfile profile) {
        if (profile == null || profile.dynamicGlossary.isEmpty()) {
            return query;
        }

        String expanded = query;
        for (Map.Entry<String, String> entry : profile.dynamicGlossary.entrySet()) {
            String acronym = entry.getKey();
            String definition = entry.getValue();

            // Se a query tem a sigla mas NÃO a definição, expandimos
            if (query.contains(acronym) && !query.toLowerCase().contains(definition.toLowerCase())) {
                // Heurística: Expandir apenas se a sigla for curta (<=4 chars) ou muito
                // específica
                // Para evitar poluição, adicionamos a definição entre parênteses
                // Ex: "O que é IDC?" -> "O que é IDC (Índice de Dispersão de Contexto)?"
                // Usando replaceFirst para não repetir se aparecer 2x
                expanded = expanded.replaceFirst(
                        "\\b" + java.util.regex.Pattern.quote(acronym) + "\\b",
                        acronym + " (" + definition + ")");
            }
        }
        return expanded;
    }

    private String enrichSystemPromptWithGlossary(String baseSystemPrompt, String userQuery,
            DocumentAnalyzer.DocumentProfile profile) {
        Map<String, String> staticDefinitions = documentAnalyzer.getTermDefinitions();
        StringBuilder glossary = new StringBuilder();
        String lowerQuery = userQuery.toLowerCase();

        // 1. Glossário Estático (term-expansions.properties)
        for (Map.Entry<String, String> entry : staticDefinitions.entrySet()) {
            if (lowerQuery.contains(entry.getKey())) {
                glossary.append("- ").append(entry.getValue()).append("\n");
            }
        }

        // 2. Glossário Dinâmico (Extraído do Documento via Layer 2)
        if (profile != null && !profile.dynamicGlossary.isEmpty()) {
            for (Map.Entry<String, String> entry : profile.dynamicGlossary.entrySet()) {
                if (userQuery.contains(entry.getKey())) { // Case sensitive para siglas costuma ser melhor? Ou não?
                    // Vamos usar contains simples por enquanto, assumindo que
                    // profile.dynamicGlossary tem keys corretas
                    glossary.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
            }
        }

        if (glossary.length() > 0) {
            return baseSystemPrompt
                    + "\n\n### GLOSSÁRIO DE DOMÍNIO E REGRAS DE NEGÓCIO:\n"
                    + "Use estas definições para entender as siglas e suas implicações nas regras do sistema:\n"
                    + glossary.toString();
        }

        return baseSystemPrompt;
    }

    private String formatEvidenceItem(Evidence e, int index) {
        String cleanedExcerpt = cleanText(e.excerpt().replaceAll("\"", "'"));
        String indexStr = index > 0 ? String.valueOf(index) : "#";
        return String.format(
                """
                        EVIDENCE_%s:
                        - doc: %s
                        - section: %s
                        - page: %d
                        - excerpt: "%s"
                        - score: %.2f
                        """,
                indexStr,
                e.documentId(),
                e.section() != null ? e.section() : "N/A",
                e.page() != null ? e.page() : 0,
                cleanedExcerpt,
                e.score());
    }

    /**
     * Remove espaços extras e quebras de linha desnecessárias para economizar
     * tokens.
     */
    private String cleanText(String text) {
        if (text == null)
            return "";
        return text.replaceAll("\\s+", " ").trim();
    }

    private CompletableFuture<AIProviderResponse> saveResponseAndUpdateProfile(
            AIProviderResponse response,
            ChatSession session,
            ChatMessage userMessage,
            Long userId,
            InteractionType type,
            List<Evidence> evidences) {
        return CompletableFuture.supplyAsync(() -> {
            String contentToSave = response.getContent();
            if (contentToSave == null || contentToSave.isBlank()) {
                log.warn("Conteúdo da resposta está vazio. Usando JSON de falha padrão.");
                contentToSave = getFailureJson();
            }

            if (type == InteractionType.RAG_ANALYSIS) {
                log.info("Resposta RAG já está em formato JSON final. Salvando diretamente.");
            }

            ChatMessage assistantMessage = chatService.saveMessage(session, "assistant", contentToSave);
            log.info("Resposta do assistente salva no banco: {}", assistantMessage.getId());

            if (type == InteractionType.CHAT || type == InteractionType.RAG_ANALYSIS) {
                List<Map<String, String>> chatHistory = List.of(
                        Map.of("role", "user", "content", userMessage.getContent()),
                        Map.of("role", "assistant", "content", contentToSave));
                memoryService.updateUserProfile(userId, chatHistory);
                log.info("Perfil do usuário atualizado para interação do tipo {}.", type);
            } else {
                log.info("Memória ignorada para interação do tipo {}", type);
            }

            response.setContent(contentToSave);
            response.setSessionId(session.getId());
            response.setEvidences(evidences);
            response.setType(type);
            return response;
        });
    }

    private CompletableFuture<AIProviderResponse> executeAndLogTask(AIProviderRequest request, AIProvider provider,
            String taskName) {
        log.debug("Enviando requisição '{}' para o provedor '{}'", taskName, provider.getClass().getSimpleName());

        // Usar o serviço de cache para executar a tarefa
        return promptCacheService.executeWithCache(provider, request)
                .handle((response, throwable) -> {
                    if (throwable != null) {
                        Throwable cause = throwable;
                        // Unpack loop para achar a causa raiz
                        while (cause.getCause() != null && cause != cause.getCause()) {
                            cause = cause.getCause();
                        }

                        // GRACEFUL DEGRADATION: Fallback para Ollama se Groq estourar budget
                        boolean isGroqBudgetError = cause.getMessage() != null
                                && (cause.getMessage().contains("Groq budget excedido")
                                        || cause.getClass().getSimpleName().contains("GroqBudgetExceeded"));

                        if (isGroqBudgetError && !provider.getClass().getSimpleName().contains("Ollama")) {
                            log.warn("⚠️ Groq Budget Excedido na tarefa '{}'. Iniciando FALLBACK para Ollama...",
                                    taskName);
                            AIProvider ollama = getProvider("ollamaProvider");
                            try {
                                return promptCacheService.executeWithCache(ollama, request).join();
                            } catch (Exception e) {
                                log.error("❌ Falha crítica no Fallback (Ollama) para '{}': {}", taskName,
                                        e.getMessage());
                                throw new RuntimeException("Falha no provedor principal e no fallback", e);
                            }
                        }

                        log.error("!!! ERRO na execução da tarefa '{}': {}", taskName, cause.getMessage(), cause);
                        throw new RuntimeException(cause);
                    } else {
                        log.info("<<< SUCESSO na tarefa '{}'. Resposta recebida.", taskName);
                        return response;
                    }
                });
    }

    private String getProviderName(String provider) {
        return (provider == null || provider.isBlank()) ? DEFAULT_PROVIDER : provider;
    }

    private AIProvider getProvider(String providerName) {
        AIProvider provider = aiProviders.get(providerName);
        if (provider == null) {
            throw new IllegalArgumentException("Provedor de IA desconhecido: " + providerName);
        }
        return provider;
    }

    private String getNotFoundJson() {
        return "{\"answer\":{\"markdown\":\"A informação solicitada não foi encontrada no documento.\",\"plainText\":\"A informação solicitada não foi encontrada no documento.\"},\"references\":[]}";
    }

    private String getFailureJson() {
        return "{\"answer\":{\"markdown\":\"❌ Erro interno ao processar a resposta da IA.\",\"plainText\":\"❌ Erro interno ao processar a resposta da IA.\"},\"references\":[]}";
    }

    public CompletableFuture<com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAIResponse> processKnowledgeAssist(
            com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAIRequest request) {
        log.info(">>> [ORCHESTRATOR] Processando Knowledge Assist: {}", request.getCommand());

        String providerName = DEFAULT_PROVIDER;
        AIProvider provider = getProvider(providerName);

        return CompletableFuture.supplyAsync(() -> {
            try {
                String prompt;
                String systemPrompt = "Você é um assistente de escrita inteligente integrado a um editor de texto (tipo Notion AI). "
                        +
                        "Sua tarefa é ajudar o usuário a escrever, editar e melhorar notas.";

                // PERSISTENCE LOGIC START
                ChatSession session = null;
                boolean isAgentMode = request
                        .getCommand() == com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAIRequest.Command.ASK_AGENT;

                if (isAgentMode && request.getKnowledgeId() != null) {
                    // Try to find existing session for this Knowledge Item
                    Optional<ChatSession> existingSession = chatSessionRepository
                            .findByKnowledgeItemId(request.getKnowledgeId());

                    if (existingSession.isPresent()) {
                        session = existingSession.get();
                        log.info("Sessão existente encontrada: {}", session.getId());
                    } else {
                        // Create new session
                        com.matheusdev.mindforge.knowledgeltem.model.KnowledgeItem item = knowledgeItemRepository
                                .findById(request.getKnowledgeId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "Knowledge Item not found: " + request.getKnowledgeId()));

                        session = new ChatSession();
                        session.setTitle("Chat: " + item.getTitle());
                        session.setCreatedAt(java.time.LocalDateTime.now());
                        session.setKnowledgeItem(item);
                        session = chatSessionRepository.save(session);
                        log.info("Nova sessão de chat criada para KnowledgeItem {}: {}", item.getId(), session.getId());
                    }
                }
                // PERSISTENCE LOGIC END

                switch (request.getCommand()) {
                    case CONTINUE -> {
                        prompt = "Continue o seguinte texto de forma natural e coesa:\n\n" + request.getContext();
                    }
                    case SUMMARIZE -> {
                        prompt = "Resuma o seguinte texto em tópicos ou parágrafos concisos:\n\n"
                                + request.getContext();
                    }
                    case FIX_GRAMMAR -> {
                        prompt = "Corrija a gramática e ortografia do seguinte texto, mantendo o tom original:\n\n"
                                + request.getContext();
                    }
                    case IMPROVE -> {
                        prompt = "Melhore a clareza, fluxo e vocabulário do seguinte texto:\n\n" + request.getContext();
                    }
                    case CUSTOM -> {
                        prompt = String.format("Instrução: %s\n\nTexto: %s", request.getInstruction(),
                                request.getContext());
                    }
                    case ASK_AGENT -> {
                        if (request.isUseContext()) {
                            log.info("🕵️ Modo Agente com Contexto (RAG)...");
                            prompt = String.format(
                                    "Aja como um agente especialista.\nContexto da nota atual:\n%s\n\nPergunta/Comando do usuário:\n%s",
                                    request.getContext(), request.getInstruction());
                        } else {
                            prompt = String.format("Pergunta: %s\n\nContexto (Nota atual): %s",
                                    request.getInstruction(), request.getContext());
                        }
                    }
                    default -> throw new IllegalArgumentException("Comando desconhecido");
                }

                if (session != null) {
                    // Save User Message
                    chatService.saveMessage(session, "user", request.getInstruction() != null ? request.getInstruction()
                            : "Comando: " + request.getCommand());
                }

                AIProviderRequest aiRequest = new AIProviderRequest(prompt, systemPrompt, null, providerName);
                AIProviderResponse response = executeAndLogTask(aiRequest, provider,
                        "Knowledge Assist - " + request.getCommand()).get();

                String resultText = response.getContent();

                if (session != null) {
                    // Save Assistant Message
                    chatService.saveMessage(session, "assistant", resultText);
                }

                return new com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAIResponse(resultText, true,
                        "Sucesso");

            } catch (Exception e) {
                log.error("Erro no Knowledge Assist", e);
                return new com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAIResponse("", false,
                        "Erro: " + e.getMessage());
            }
        });
    }
}
