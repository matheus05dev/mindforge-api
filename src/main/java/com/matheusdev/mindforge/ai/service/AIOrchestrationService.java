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
/**
 * Serviço central de orquestração de Inteligência Artificial.
 * <p>
 * Responsável por gerenciar o fluxo completo de requisições de IA, incluindo:
 * <ul>
 * <li>Roteamento inteligente entre provedores (Local vs Cloud).</li>
 * <li>Gerenciamento de contexto e sessões de chat.</li>
 * <li>Execução de pipelines RAG (Retrieval-Augmented Generation).</li>
 * <li>Análise de documentos (One-Shot, Map-Reduce).</li>
 * <li>Integração com serviços de memória e aprendizado do usuário.</li>
 * </ul>
 * atua como o "cérebro" que coordena os diversos componentes do sistema
 * MindForge.
 */
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
    private final com.matheusdev.mindforge.study.note.repository.StudyNoteRepository studyNoteRepository;
    private final com.matheusdev.mindforge.ai.chat.repository.ChatSessionRepository chatSessionRepository;
    private final com.matheusdev.mindforge.knowledgeltem.service.ProposalCacheService proposalCacheService;

    private static final String DEFAULT_PROVIDER = "ollamaProvider";
    private static final String FALLBACK_PROVIDER = "groqProvider";
    private static final int OLLAMA_CHAR_LIMIT = 5000;
    private static final long MAX_VALID_SESSION_ID = 1_000_000_000L;
    private final Semaphore semaphore = new Semaphore(1);

    /**
     * Executa uma análise interna sem interação direta do usuário (headless).
     * Útil para processos de background como geração de títulos, resumos
     * automáticos, etc.
     *
     * @param prompt        Prompt principal com a instrução.
     * @param systemMessage Mensagem de sistema para definir o comportamento da IA.
     * @return CompletableFuture com a resposta do provedor.
     */
    public CompletableFuture<AIProviderResponse> executeInternalAnalysis(String prompt, String systemMessage) {
        log.info(">>> [ORCHESTRATOR] Executando análise interna (headless)...");

        // CORREÇÃO: Revertido para Default (Ollama) para economizar tokens do Groq.
        // O usuário prefere processamento local para tarefas de background (headless).
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

    /**
     * Gerencia a interação completa de chat com o usuário.
     * O fluxo inclui:
     * Verificação/Criação de sessão.
     * Persistência da mensagem do usuário.
     * Recuperação de contexto (RAG) se aplicável.
     * Construção dinâmica de prompt (Persona, Glossário).
     * Execução via provedor de IA.
     * Persistência da resposta e atualização de memória assíncrona.
     *
     * @param chatRequest DTO contendo a mensagem e metadados da requisição.
     * @return CompletableFuture com a resposta estruturada.
     */
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

        // CORREÇÃO: Recuperação de Contexto
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
                            null))
                    .thenApply(response -> {
                        // TRIGGER AUTO-RENAME BACKGROUND TASK
                        // If title is "Nova Conversa" and it's a chat interaction, generate a title
                        if ("Nova Conversa".equals(session.getTitle())) {
                            triggerBackgroudTitleGeneration(session.getId(), userPrompt);
                        }
                        return response;
                    });
        }
    }

    /**
     * Dispara uma tarefa em background para gerar um título curto para a sessão
     * baseado na primeira mensagem do usuário.
     *
     * @param sessionId    ID da sessão a ser renomeada.
     * @param firstMessage Conteúdo da primeira mensagem para basear o título.
     */
    private void triggerBackgroudTitleGeneration(Long sessionId, String firstMessage) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info(">>> [AUTO-RENAME] Gerando título para sessão {}", sessionId);
                String prompt = String.format(
                        "Gere um título muito curto (3 a 5 palavras max), resumido e sem aspas para uma conversa que começa com: \"%s\". Responda APENAS o título, nada mais.",
                        firstMessage.length() > 200 ? firstMessage.substring(0, 200) + "..." : firstMessage);

                // Usa o provedor padrão (geralmente Ollama/Llama) para esta tarefa rápida
                AIProvider provider = getProvider(DEFAULT_PROVIDER);
                AIProviderRequest request = new AIProviderRequest(prompt,
                        "Você é um assistente especializado em resumir tópicos.", null, DEFAULT_PROVIDER);

                // Executa e esquece (fire and forget), apenas logando falhas
                promptCacheService.executeWithCache(provider, request).thenAccept(response -> {
                    String newTitle = response.getContent();
                    if (newTitle != null) {
                        newTitle = newTitle.replace("\"", "").replace("'", "").trim();
                        if (!newTitle.isBlank()) {
                            chatService.updateSessionTitle(sessionId, newTitle);
                            log.info(">>> [AUTO-RENAME] Sessão {} renomeada para: {}", sessionId, newTitle);
                        }
                    }
                }).exceptionally(ex -> {
                    log.warn("Falha ao gerar título automático: {}", ex.getMessage());
                    return null;
                });

            } catch (Exception e) {
                log.warn("Erro ao iniciar task de auto-rename: {}", e.getMessage());
            }
        });
    }

    /**
     * Processa a análise de arquivos enviados pelo usuário. *
     * Suporta:
     * Extração de texto via Tika (PDF, DOCX, etc).
     * Análise multimodal para imagens.
     * Roteamento inteligente de estratégia (One-Shot vs Map-Reduce vs
     * RAG).
     * Fallback automático de provedores baseado em tamanho e tipo.
     *
     * @param userPrompt   Prompt do usuário sobre o arquivo.
     * @param providerName Nome do provedor forçado (opcional).
     * @param file         Arquivo enviado.
     * @return CompletableFuture com a resposta da análise.
     * @throws IOException Se houver erro na leitura do arquivo.
     */
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
                    .map(springDoc -> Document.from(springDoc.getText(), new Metadata(springDoc.getMetadata())))
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

    /**
     * Processa chunks de texto sequencialmente.
     * Otimizado para provedores locais (Ollama) onde não há limite restrito de
     * taxa,
     * mas o hardware é o gargalo.
     */
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

    /**
     * Processa chunks de texto com controle de taxa (Rate Limiting) e semáforo.
     * Otimizado para provedores de nuvem (Groq) para respeitar limites de
     * tokens/minuto (TPM).
     */
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

    /**
     * Combina (reduz) os resultados parciais dos chunks em uma resposta final
     * coerente.
     *
     * @param partialResults Lista de resumos/análises de cada chunk.
     * @param basePrompts    Prompts base.
     * @param provider       Provedor de IA.
     * @param providerName   Nome do provedor.
     * @param userPrompt     Prompt original do usuário.
     * @return Resposta consolidada.
     */
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

    /**
     * Processa documentos pequenos que cabem inteiros no contexto (One-Shot).
     * Envia o documento completo e o prompt em uma única chamada.
     */
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

    /**
     * Executa o fluxo de RAG (Retrieval-Augmented Generation) em duas etapas:
     * 1. Extração: Identifica trechos relevantes e respostas potenciais.
     * 2. Auditoria/Síntese: Verifica se a resposta foi realmente encontrada e
     * formata com referências.
     *
     * @param documentId   ID do documento no Vector Store.
     * @param document     Conteúdo bruto (opcional neste ponto).
     * @param userPrompt   Pergunta do usuário.
     * @param basePrompts  Prompts base.
     * @param provider     Provedor de IA.
     * @param providerName Nome do provedor.
     * @param evidences    Lista de evidências recuperadas pelo serviço de busca
     *                     vetorial.
     * @param docProfile   Perfil do documento (para glossário dinâmico).
     * @return Future com a resposta auditada.
     */
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
                        0.2, // Temperatura baixa para precisão
                        null // maxTokens
                );

                log.info("📤 Etapa 1: Enviando prompt de extração para '{}'.", providerName);
                AIProviderResponse extractionResponse = executeAndLogTask(extractionRequest, provider, "Extração RAG")
                        .get();
                String extractedContent = extractionResponse.getContent();

                log.info("✅ Etapa 1: Resposta de extração recebida.");

                AuditedAnswer finalAnswer;
                // CORREÇÃO: A lógica anterior descartava respostas parciais se contivesse a
                // frase.
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

    /**
     * Formata e seleciona as evidências para o contexto do RAG, priorizando tabelas
     * e métricas.
     * Limita o tamanho total em caracteres para caber no contexto.
     *
     * @param evidences Lista completa de evidências encontradas.
     * @return String formatada com as evidências selecionadas.
     */
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

        // Estratégia: Preencher com Prioritárias primeiro, depois Standard, então
        // Reordenar pelo índice original.

        // Adiciona Evidências Prioritárias
        for (Evidence e : priorityEvidences) {
            String formatted = formatEvidenceItem(e, -1);
            if (currentLength + formatted.length() <= MAX_EVIDENCE_CHARS) {
                selectedEvidences.add(e);
                currentLength += formatted.length();
            } else {
                log.warn("⚠️ Tabela crítica ignorada por falta de espaço no limite de {} chars!", MAX_EVIDENCE_CHARS);
            }
        }

        // Adiciona Evidências Padrão (até o limite)
        for (Evidence e : standardEvidences) {
            String formatted = formatEvidenceItem(e, -1);
            if (currentLength + formatted.length() <= MAX_EVIDENCE_CHARS) {
                selectedEvidences.add(e);
                currentLength += formatted.length();
            }
        }

        // Reordena lista para o fluxo original (Interseção de Original & Selecionados)
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

    /**
     * Expande a query do usuário com termos do glossário dinâmico.
     * Ex: "O que é IDC?" -> "O que é IDC (Índice de Dispersão de Contexto)?"
     */
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

    /**
     * Enriquece o System Prompt com o glossário de domínio (estático e dinâmico).
     * Fornece definições contextuais para a IA.
     */
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

                        // GRACEFUL DEGRADATION: Fallback para Ollama em QUALQUER erro do Groq
                        boolean isOllama = provider.getClass().getSimpleName().toLowerCase().contains("ollama");

                        if (!isOllama) {
                            log.warn("⚠️ Falha no provedor principal '{}'. Erro: {}. Iniciando FALLBACK para Ollama...",
                                    taskName, cause.getMessage());
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

    /**
     * Processa requisições de assistência de conhecimento (Knowledge Assist).
     * Suporta comandos como CONTINUE, SUMMARIZE, FIX_GRAMMAR, etc.
     *
     * @param request DTO da requisição.
     * @return Future com a resposta do assistente.
     */
    public CompletableFuture<com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAIResponse> processKnowledgeAssist(
            com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAIRequest request) {
        log.info(">>> [ORCHESTRATOR] Processando Knowledge Assist: {}", request.getCommand());

        String providerName = FALLBACK_PROVIDER;
        AIProvider provider = getProvider(providerName);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // VERIFICAÇÃO DE MODO AGENTE
                if (request.isAgentMode() && request.getKnowledgeId() != null) {
                    log.info("🤖 AGENT MODE ATIVADO para Knowledge Item {}", request.getKnowledgeId());
                    return processAgentMode(request, provider, providerName);
                }

                // LÓGICA ORIGINAL DE MODO PENSAMENTO (THINKING MODE)
                String prompt;
                String systemPrompt = "Você é um assistente de escrita inteligente integrado a um editor de texto (tipo Notion AI). "
                        +
                        "Sua tarefa é ajudar o usuário a escrever, editar e melhorar notas.";

                // INÍCIO DA LÓGICA DE PERSISTÊNCIA
                ChatSession session = null;
                boolean isAgentMode = request
                        .getCommand() == com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAIRequest.Command.ASK_AGENT;

                if (isAgentMode && request.getKnowledgeId() != null) {
                    // Tenta encontrar sessão existente para este Knowledge Item
                    Optional<ChatSession> existingSession = chatSessionRepository
                            .findByKnowledgeItemId(request.getKnowledgeId());

                    if (existingSession.isPresent()) {
                        session = existingSession.get();
                        log.info("Sessão existente encontrada: {}", session.getId());
                    } else {
                        // Cria nova sessão
                        com.matheusdev.mindforge.knowledgeltem.model.KnowledgeItem item = knowledgeItemRepository
                                .findById(request.getKnowledgeId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "Knowledge Item não encontrado: " + request.getKnowledgeId()));

                        session = new ChatSession();
                        session.setTitle("Chat: " + item.getTitle());
                        session.setCreatedAt(java.time.LocalDateTime.now());
                        session.setKnowledgeItem(item);
                        session = chatSessionRepository.save(session);
                        log.info("Nova sessão de chat criada para KnowledgeItem {}: {}", item.getId(), session.getId());
                    }
                }
                // FIM DA LÓGICA DE PERSISTÊNCIA

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
                            log.info("🕵️ Modo Agente com Contexto (RAG) - Buscando em outras notas...");

                            // Executa busca RAG em TODOS os itens de conhecimento
                            String searchQuery = request.getInstruction();
                            StringBuilder ragEvidence = new StringBuilder();

                            try {
                                // Recupera todos os itens para busca cruzada
                                List<com.matheusdev.mindforge.knowledgeltem.model.KnowledgeItem> allItems = knowledgeItemRepository
                                        .findAll();

                                List<Evidence> allEvidences = new ArrayList<>();

                                // Busca em cada item indexado
                                for (var item : allItems) {
                                    // Pula a nota atual (já está no contexto)
                                    if (request.getKnowledgeId() != null
                                            && item.getId().equals(request.getKnowledgeId())) {
                                        continue;
                                    }

                                    String docId = "knowledge_" + item.getId();
                                    try {
                                        List<Evidence> evidences = ragService.queryIndexedDocument(docId, searchQuery,
                                                3);
                                        for (Evidence ev : evidences) {
                                            if (ev.score() >= 0.75) { // Apenas resultados de alta relevância
                                                allEvidences.add(ev);
                                            }
                                        }
                                    } catch (Exception e) {
                                        log.debug("Documento {} não indexado ou erro: {}", docId, e.getMessage());
                                    }
                                }

                                // Classifica por score e pega o top 5
                                allEvidences.sort((a, b) -> Double.compare(b.score(), a.score()));
                                List<Evidence> topEvidences = allEvidences.stream().limit(5).toList();

                                if (!topEvidences.isEmpty()) {
                                    ragEvidence.append("\n\n### EVIDÊNCIAS DE OUTRAS NOTAS:\n");
                                    for (Evidence ev : topEvidences) {
                                        ragEvidence.append(String.format("\n**Fonte: %s** (Relevância: %.2f)\n%s\n",
                                                ev.documentId().replace("knowledge_", "Nota #"),
                                                ev.score(),
                                                ev.excerpt()));
                                    }
                                    log.info("✅ RAG encontrou {} evidências relevantes", topEvidences.size());
                                } else {
                                    log.warn("⚠️ RAG não encontrou evidências relevantes para: {}", searchQuery);
                                }
                            } catch (Exception e) {
                                log.error("❌ Erro ao buscar evidências RAG", e);
                            }

                            prompt = String.format(
                                    "Você é um agente especialista que tem acesso a uma base de conhecimento.\n\n" +
                                            "**CONTEXTO DA NOTA ATUAL:**\n%s\n\n" +
                                            "%s\n\n" +
                                            "**PERGUNTA/COMANDO DO USUÁRIO:**\n%s\n\n" +
                                            "Responda de forma precisa, citando as fontes quando usar informações das evidências.",
                                    request.getContext(),
                                    ragEvidence.length() == 0 ? "(Nenhuma evidência adicional encontrada)"
                                            : ragEvidence.toString(),
                                    request.getInstruction());
                        } else {
                            prompt = String.format("Pergunta: %s\n\nContexto (Nota atual): %s",
                                    request.getInstruction(), request.getContext());
                        }
                    }
                    case AGENT_UPDATE -> {
                        // This should be handled by agent mode, but if not, treat as CUSTOM
                        prompt = String.format("Instrução: %s\n\nTexto: %s", request.getInstruction(),
                                request.getContext());
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

    /**
     * Processa requisição em MODO AGENTE - gera propostas de diff estruturadas.
     */
    private com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAIResponse processAgentMode(
            com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAIRequest request,
            AIProvider provider,
            String providerName) {

        try {
            // 1. RECUPERAR CONTEÚDO DO BANCO (fonte da verdade, não do request)
            com.matheusdev.mindforge.knowledgeltem.model.KnowledgeItem item = knowledgeItemRepository
                    .findById(request.getKnowledgeId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Knowledge Item não encontrado: " + request.getKnowledgeId()));

            String currentContent = item.getContent() != null ? item.getContent() : "";
            log.info("📄 Conteúdo recuperado do banco: {} caracteres", currentContent.length());

            // 2. CONSTRUIR PROMPT PARA GERAÇÃO DE DIFF
            String systemPrompt = """
                    Você é um arquiteto de conhecimento e editor sênior.
                    Sua tarefa é analisar a estrutura do documento e propor mudanças cirúrgicas e contextuais.

                    **DIRETRIZES DE PROPOSICIONAMENTO (CONTEXT-AWARE):**
                    1. **Análise Estrutural**: Entenda a hierarquia de títulos (#, ##, ###) e o fluxo lógico do texto.
                    2. **Inserção Contextual**: NUNCA insira conteúdo aleatoriamente no topo ou fim, a menos que seja uma introdução ou conclusão.
                       - Se o usuário pede "Microservices" em um doc de "Java", procure seção "Avançado", "Arquitetura" ou crie uma nova seção no final se for um tópico avançado.
                       - Se for um conceito básico, insira nas seções iniciais.
                    3. **Granularidade**: Se for uma correção pequena, substitua apenas a frase/parágrafo. Se for um tópico novo, adicione a seção completa.

                    **FORMATO DE RESPOSTA:**
                    Retorne APENAS um JSON válido.
                    IMPORTANTE: Para strings multilinha, utilize "\\n" explícito. NÃO utilize backticks (`) para envolver strings, isso quebra o parser JSON.
                    Exemplo CORRETO: "proposedContent": "Linha 1\\nLinha 2"
                    Exemplo INCORRETO: "proposedContent": `Linha 1
                    Linha 2`

                    Formato JSON esperado:
                    {
                      "summary": "Explique POR QUE escolheu este local para inserção",
                      "changes": [
                        {
                          "type": "ADD", // UM DE: 'ADD', 'REMOVE', 'REPLACE'
                          "startLine": número_da_linha_inicial,
                          "endLine": número_da_linha_final,
                          "originalContent": "conteúdo original (vazio para ADD)",
                          "proposedContent": "novo conteúdo (vazio para REMOVE) - USE APENAS ASPAS DUPLAS E ESCAPE QUEBRAS DE LINHA",
                          "reason": "Justificativa da posição (ex: 'Inserido após seção de Arrays por ser um tópico relacionado')"
                        }
                      ]
                    }
                    """;

            String userPrompt = String.format("""
                    CONTEÚDO ATUAL:
                    ```
                    %s
                    ```

                    INSTRUÇÃO DO USUÁRIO:
                    %s

                    Gere as mudanças necessárias em formato JSON.
                    """, currentContent, request.getInstruction());

            // 3. CHAMADA DE IA PARA GERAR DIFF
            AIProviderRequest aiRequest = new AIProviderRequest(
                    userPrompt,
                    systemPrompt,
                    null,
                    providerName,
                    false,
                    null,
                    null,
                    null,
                    null,
                    0.3, // Temperatura baixa para output estruturado
                    4096 // Limite maior para diffs do Modo Agente
            );

            AIProviderResponse aiResponse = executeAndLogTask(aiRequest, provider, "Geração de Diff (Agente)").get();
            String jsonResponse = aiResponse.getContent();

            log.info("🤖 AI Response: {}", jsonResponse);

            // 4. PARSE DA RESPOSTA JSON
            String cleanJson = sanitizeResponse(jsonResponse);
            com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAgentProposal proposal = parseAgentProposal(
                    cleanJson,
                    request.getKnowledgeId(),
                    currentContent);

            // 5. ARMAZENAR PROPOSTA EM CACHE
            String proposalId = proposalCacheService.storeProposal(proposal);
            log.info("✅ Proposta {} armazenada para o item {}", proposalId, request.getKnowledgeId());

            // 6. RETORNAR PROPOSTA AO FRONTEND
            com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAIResponse response = new com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAIResponse(
                    null, true, "Proposta gerada com sucesso");
            response.setProposal(proposal);

            return response;

        } catch (Exception e) {
            log.error("❌ Erro no Agent Mode", e);
            return new com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAIResponse(
                    "",
                    false,
                    "Erro ao gerar proposta: " + e.getMessage());
        }
    }

    /**
     * Sanitiza a resposta da IA para corrigir problemas comuns de JSON (backticks,
     * blocos markdown).
     */
    private String sanitizeResponse(String jsonResponse) {
        String clean = jsonResponse.trim();

        // FIX: Extrai parte JSON se houver texto conversacional
        int firstBrace = clean.indexOf("{");
        int lastBrace = clean.lastIndexOf("}");

        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            clean = clean.substring(firstBrace, lastBrace + 1);
        }

        // Remove blocos de markdown se existirem
        if (clean.startsWith("```json")) {
            clean = clean.substring(7);
        } else if (clean.startsWith("```")) {
            clean = clean.substring(3);
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length() - 3);
        }
        clean = clean.trim();

        // FIX: Substitui backticks usados como aspas por aspas duplas
        // Padrão: busca `...` que contém quebras de linha ou é usado como valor
        // Usamos regex específico para converter strings com backtick para strings JSON
        // válidas
        // DOTALL adicionado para suportar strings multilinha dentro de backticks
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("`([^`]*)`", java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher matcher = pattern.matcher(clean);

        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String content = matcher.group(1);
            // Escape double quotes and newlines inside the content
            String escaped = content
                    .replace("\\", "\\\\") // Escape backslashes first
                    .replace("\"", "\\\"") // Escape quotes
                    .replace("\n", "\\n") // Escape newlines to literal \n
                    .replace("\r", ""); // Remove carriage returns

            // Use quoteReplacement to handle $ and \ correctly in appendReplacement
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement("\"" + escaped + "\""));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Faz o parse da resposta JSON da IA para o objeto KnowledgeAgentProposal.
     */
    private com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAgentProposal parseAgentProposal(
            String jsonResponse,
            Long knowledgeId,
            String originalContent) throws Exception {

        // Parse JSON
        com.fasterxml.jackson.databind.JsonNode root = objectMapper.reader()
                .with(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS)
                .readTree(jsonResponse);

        String summary = root.has("summary") ? root.get("summary").asText() : "Mudanças propostas";
        List<com.matheusdev.mindforge.knowledgeltem.dto.ContentChange> changes = new ArrayList<>();

        if (root.has("changes") && root.get("changes").isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode changeNode : root.get("changes")) {
                com.matheusdev.mindforge.knowledgeltem.dto.ContentChange change = new com.matheusdev.mindforge.knowledgeltem.dto.ContentChange();

                change.setType(com.matheusdev.mindforge.knowledgeltem.dto.ContentChange.ChangeType.valueOf(
                        changeNode.get("type").asText()));
                change.setStartLine(changeNode.has("startLine") ? changeNode.get("startLine").asInt() : 0);
                change.setEndLine(changeNode.has("endLine") ? changeNode.get("endLine").asInt() : 0);
                change.setOriginalContent(
                        changeNode.has("originalContent") ? changeNode.get("originalContent").asText() : "");
                change.setProposedContent(
                        changeNode.has("proposedContent") ? changeNode.get("proposedContent").asText() : "");
                change.setReason(changeNode.has("reason") ? changeNode.get("reason").asText() : "");

                changes.add(change);
            }
        }

        com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAgentProposal proposal = new com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAgentProposal();
        proposal.setKnowledgeId(knowledgeId);
        proposal.setSummary(summary);
        proposal.setChanges(changes);
        proposal.setCreatedAt(java.time.LocalDateTime.now());
        proposal.setOriginalContent(originalContent);

        return proposal;
    }

    /**
     * Processa requisições de assistência de Notas de Estudo (Study Note Assist).
     * Espelha a funcionalidade do Knowledge Assist.
     */
    public CompletableFuture<com.matheusdev.mindforge.study.note.dto.StudyNoteAIResponse> processStudyNoteAssist(
            com.matheusdev.mindforge.study.note.dto.StudyNoteAIRequest request) {
        log.info(">>> [ORCHESTRATOR] Processando Study Note Assist: {}", request.getCommand());

        String providerName = FALLBACK_PROVIDER;
        AIProvider provider = getProvider(providerName);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // VERIFICAÇÃO DE MODO AGENTE
                if (request.isAgentMode() && request.getNoteId() != null) {
                    log.info("🤖 AGENT MODE ATIVADO para Study Note {}", request.getNoteId());
                    return processStudyNoteAgentMode(request, provider, providerName);
                }

                String prompt;
                String systemPrompt = "Você é um assistente de estudo inteligente integrado a um editor de notas. " +
                        "Sua tarefa é ajudar o estudante a resumir, expandir e melhorar suas anotações de estudo.";

                // INÍCIO DA LÓGICA DE PERSISTÊNCIA
                ChatSession session = null;
                // Notas usam ASK_AGENT ou similar para conversas? Sim.
                boolean isAgentMode = request
                        .getCommand() == com.matheusdev.mindforge.study.note.dto.StudyNoteAIRequest.Command.ASK_AGENT;

                if (isAgentMode && request.getNoteId() != null) {
                    // Tenta encontrar sessão existente para esta Nota
                    Optional<ChatSession> existingSession = chatSessionRepository
                            .findByStudyNoteId(request.getNoteId());

                    if (existingSession.isPresent()) {
                        session = existingSession.get();
                        log.info("Sessão existente encontrada para Nota: {}", session.getId());
                    } else {
                        // Cria nova sessão
                        com.matheusdev.mindforge.study.note.model.Note note = studyNoteRepository
                                .findById(request.getNoteId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "Note não encontrada: " + request.getNoteId()));

                        session = new ChatSession();
                        session.setTitle("Chat: " + note.getTitle());
                        session.setCreatedAt(java.time.LocalDateTime.now());
                        session.setStudyNote(note);
                        session = chatSessionRepository.save(session);
                        log.info("Nova sessão de chat criada para Nota {}: {}", note.getId(), session.getId());
                    }
                }

                switch (request.getCommand()) {
                    case CONTINUE -> {
                        prompt = "Continue a seguinte anotação de estudo de forma didática e coesa:\n\n"
                                + request.getContext();
                    }
                    case SUMMARIZE -> {
                        prompt = "Resuma a seguinte anotação em tópicos principais para revisão rápida:\n\n"
                                + request.getContext();
                    }
                    case FIX_GRAMMAR -> {
                        prompt = "Corrija a gramática e melhore a clareza da seguinte anotação:\n\n"
                                + request.getContext();
                    }
                    case IMPROVE -> {
                        prompt = "Melhore a explicação e o conteúdo desta anotação, tornando-a mais completa e fácil de estudar:\n\n"
                                + request.getContext();
                    }
                    case CUSTOM -> {
                        prompt = String.format("Instrução: %s\n\nTexto: %s", request.getInstruction(),
                                request.getContext());
                    }
                    case ASK_AGENT -> {
                        if (request.isUseContext()) {
                            log.info("🕵️ Modo Agente (Study) com Contexto (RAG) - Buscando em outras notas...");

                            String searchQuery = request.getInstruction();
                            StringBuilder ragEvidence = new StringBuilder();

                            try {
                                // Recupera todas as notas para busca cruzada (simplificado, ideal seria vector
                                // search direto)
                                // Aqui assumimos que não temos índice vetorial de notas separado ainda,
                                // então buscamos apenas contexto básico ou placeholder.
                                // TODO: Implementar busca vetorial real para notas de estudo.
                                // Por enquanto, vamos buscar outras notas da mesma matéria se possível ou
                                // apenas placeholder.

                                // Placeholder: Buscar notas da mesma matéria (se disponível no repo)
                                // List<Note> subjectNotes = studyNoteRepository.findBySubjectId(...);
                                // Precisaria carregar a nota atual para saber o subject.

                                com.matheusdev.mindforge.study.note.model.Note currentNote = studyNoteRepository
                                        .findById(request.getNoteId()).orElse(null);
                                if (currentNote != null && currentNote.getSubject() != null) {
                                    List<com.matheusdev.mindforge.study.note.model.Note> siblingNotes = studyNoteRepository
                                            .findBySubjectId(currentNote.getSubject().getId());

                                    int count = 0;
                                    for (com.matheusdev.mindforge.study.note.model.Note sn : siblingNotes) {
                                        if (!sn.getId().equals(currentNote.getId()) && count < 3) {
                                            ragEvidence.append(String.format("\n**Outra Nota: %s**\n%s...\n",
                                                    sn.getTitle(), sn.getContent().substring(0,
                                                            Math.min(sn.getContent().length(), 200))));
                                            count++;
                                        }
                                    }
                                }

                                if (ragEvidence.length() > 0) {
                                    log.info("✅ Contexto de outras notas adicionado.");
                                } else {
                                    log.warn("⚠️ Sem contexto adicional relevante encontrado.");
                                }

                            } catch (Exception e) {
                                log.error("❌ Erro ao buscar contexto de notas", e);
                            }

                            prompt = String.format(
                                    "Você é um tutor inteligente ajudando nos estudos.\n\n" +
                                            "**CONTEXTO DA NOTA ATUAL:**\n%s\n\n" +
                                            "%s\n\n" +
                                            "**PERGUNTA/COMANDO DO USUÁRIO:**\n%s\n\n" +
                                            "Responda de forma didática.",
                                    request.getContext(),
                                    ragEvidence.length() == 0 ? "(Nenhum contexto extra)"
                                            : "### CONTEXTO RELACIONADO:\n" + ragEvidence.toString(),
                                    request.getInstruction());
                        } else {
                            prompt = String.format("Pergunta: %s\n\nContexto (Nota atual): %s",
                                    request.getInstruction(), request.getContext());
                        }
                    }
                    default -> throw new IllegalArgumentException("Comando desconhecido");
                }

                if (session != null) {
                    chatService.saveMessage(session, "user", request.getInstruction() != null ? request.getInstruction()
                            : "Comando: " + request.getCommand());
                }

                AIProviderRequest aiRequest = new AIProviderRequest(prompt, systemPrompt, null, providerName);
                AIProviderResponse response = executeAndLogTask(aiRequest, provider,
                        "Study Note Assist - " + request.getCommand()).get();

                String resultText = response.getContent();

                if (session != null) {
                    chatService.saveMessage(session, "assistant", resultText);
                }

                return new com.matheusdev.mindforge.study.note.dto.StudyNoteAIResponse(resultText, true,
                        "Sucesso");

            } catch (Exception e) {
                log.error("Erro no Study Note Assist", e);
                return new com.matheusdev.mindforge.study.note.dto.StudyNoteAIResponse("", false,
                        "Erro: " + e.getMessage());
            }
        });
    }

    /**
     * Processa requisição de Nota de Estudo em MODO AGENTE - gera propostas de diff
     * estruturadas.
     * Reutiliza a estrutura de KnowledgeAgentProposal.
     */
    private com.matheusdev.mindforge.study.note.dto.StudyNoteAIResponse processStudyNoteAgentMode(
            com.matheusdev.mindforge.study.note.dto.StudyNoteAIRequest request,
            AIProvider provider,
            String providerName) {

        try {
            // 1. RECUPERAR CONTEÚDO DO BANCO
            com.matheusdev.mindforge.study.note.model.Note note = studyNoteRepository
                    .findById(request.getNoteId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Note não encontrada: " + request.getNoteId()));

            String currentContent = note.getContent() != null ? note.getContent() : "";
            log.info("📄 Conteúdo recuperado da Nota {}: {} caracteres", note.getId(), currentContent.length());

            // 2. CONSTRUIR PROMPT PARA GERAÇÃO DE DIFF (Adaptado para contexto de estudo)
            String systemPrompt = """
                    Você é um especialista em educação e edição de material didático.
                    Sua tarefa é analisar a anotação do estudante e propor melhorias estruturais, correções ou expansões de conteúdo.

                    **DIRETRIZES DE PROPOSICIONAMENTO (CONTEXT-AWARE):**
                    1. **Clareza e Didática**: Melhore explicações confusas, adicione exemplos onde faltar.
                    2. **Inserção Contextual**: Se adicionar conteúdo, coloque-o na seção logicamente correta. Se for um tópico novo, crie uma seção no final ou onde fizer sentido semanticamente.
                    3. **Granularidade**: Mantenha o estilo de anotação (tópicos, negrito, emojis se já usados).

                    **FORMATO DE RESPOSTA:**
                    Retorne APENAS um JSON válido seguindo estritamente este formato.
                    IMPORTANTE: Para strings multilinha, utilize "\\n" explícito. NÃO utilize backticks (`) para envolver strings.
                    Use aspas duplas para todas as chaves e valores string.

                    Formato JSON esperado:
                    {
                      "summary": "Resumo curto do que foi alterado e porquê",
                      "changes": [
                        {
                          "type": "ADD", // UM DE: 'ADD', 'REMOVE', 'REPLACE'
                          "startLine": número_da_linha_inicial,
                          "endLine": número_da_linha_final,
                          "originalContent": "conteúdo original (vazio para ADD)",
                          "proposedContent": "novo conteúdo (vazio para REMOVE) - ESCAPE QUEBRAS DE LINHA COM \\n",
                          "reason": "Justificativa didática"
                        }
                      ]
                    }
                    """;

            String userPrompt = String.format("""
                    CONTEÚDO ATUAL DA NOTA:
                    ```
                    %s
                    ```

                    INSTRUÇÃO DO ESTUDANTE:
                    %s

                    Gere as mudanças necessárias em formato JSON.
                    """, currentContent, request.getInstruction());

            // 3. CHAMADA DE IA PARA GERAR DIFF
            AIProviderRequest aiRequest = new AIProviderRequest(
                    userPrompt,
                    systemPrompt,
                    null,
                    providerName,
                    false,
                    null,
                    null,
                    null,
                    null,
                    0.3, // Temperatura baixa para output estruturado
                    4096 // Limite maior
            );

            AIProviderResponse aiResponse = executeAndLogTask(aiRequest, provider, "Geração de Diff (Study Agent)")
                    .get();
            String jsonResponse = aiResponse.getContent();

            log.info("🤖 Study Agent Response: {}", jsonResponse);

            // 4. PARSE DA RESPOSTA JSON
            String cleanJson = sanitizeResponse(jsonResponse);

            // Reutiliza o parser de KnowledgeAgentProposal, pois a estrutura JSON é
            // idêntica
            // Precisamos apenas adaptar o ID (knowledgeId vs noteId) no objeto retornado
            // Usamos um ID fictício ou adaptamos o método de parse

            com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAgentProposal proposal = parseAgentProposal(
                    cleanJson,
                    0L, // KnowledgeId ignorado aqui
                    currentContent);

            // Ajuste manual: setar o campo transient ou apenas usar o DTO genérico
            // O DTO KnowledgeAgentProposal tem knowledgeId, mas podemos ignorar ou usar
            // noteId se o front souber lidar
            proposal.setKnowledgeId(request.getNoteId()); // Reutilizando campo ID para ID da nota

            // 5. ARMAZENAR PROPOSTA EM CACHE
            // Usamos o mesmo cache service? Sim, se ele usar String ID ou similar.
            // O cache service usa UUID na memória, então ok.
            String proposalId = proposalCacheService.storeProposal(proposal);
            log.info("✅ Proposta {} armazenada para a Nota {}", proposalId, request.getNoteId());

            // 6. RETORNAR PROPOSTA AO FRONTEND
            com.matheusdev.mindforge.study.note.dto.StudyNoteAIResponse response = new com.matheusdev.mindforge.study.note.dto.StudyNoteAIResponse(
                    null, true, "Proposta gerada com sucesso");
            response.setProposal(proposal);

            return response;

        } catch (Exception e) {
            log.error("❌ Erro no Study Agent Mode", e);
            return new com.matheusdev.mindforge.study.note.dto.StudyNoteAIResponse(
                    "",
                    false,
                    "Erro ao gerar proposta: " + e.getMessage());
        }
    }
}
