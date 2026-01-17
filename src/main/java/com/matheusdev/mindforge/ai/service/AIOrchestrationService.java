package com.matheusdev.mindforge.ai.service;

import com.matheusdev.mindforge.ai.chat.model.ChatMessage;
import com.matheusdev.mindforge.ai.chat.model.ChatSession;
import com.matheusdev.mindforge.ai.dto.ChatRequest;
import com.matheusdev.mindforge.ai.dto.PromptPair;
import com.matheusdev.mindforge.ai.memory.model.UserProfileAI;
import com.matheusdev.mindforge.ai.memory.service.MemoryService;
import com.matheusdev.mindforge.ai.provider.AIProvider;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

/**
 * Serviço central que orquestra as interações com IA.
 * Responsável por escolher o provedor, gerenciar o fluxo de arquivos (Map-Reduce)
 * e integrar com a memória do usuário.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIOrchestrationService {

    private final Map<String, AIProvider> aiProviders;
    private final MemoryService memoryService;
    private final PromptBuilderService promptBuilderService;
    private final ChatService chatService;
    
    // Define o provedor padrão caso nenhum seja especificado
    private static final String DEFAULT_PROVIDER = "ollamaProvider";
    
    // Semáforo para limitar o número de chamadas simultâneas à IA (evita sobrecarga)
    private final Semaphore semaphore = new Semaphore(3); 

    /**
     * Gerencia uma interação simples de chat (texto -> texto).
     */
    public CompletableFuture<AIProviderResponse> handleChatInteraction(ChatRequest chatRequest) {
        log.info(">>> [ORCHESTRATOR] Iniciando interação de chat.");
        
        String providerName = getProviderName(chatRequest.provider());
        AIProvider selectedProvider = getProvider(providerName);

        log.info("Provedor selecionado: {}", providerName);

        AIProviderRequest request = new AIProviderRequest(
                chatRequest.prompt(),
                chatRequest.systemMessage(),
                chatRequest.model(),
                providerName
        );

        return executeAndLogTask(request, selectedProvider, "chat interaction");
    }

    /**
     * Gerencia a análise de arquivos (PDF, Imagens, etc.).
     * Se for texto longo, aplica o padrão Map-Reduce (divide, processa, junta).
     * Salva as mensagens no banco de dados para RAG.
     */
    public CompletableFuture<AIProviderResponse> handleFileAnalysis(String userPrompt, String providerName, MultipartFile file) throws IOException {
        log.info(">>> [ORCHESTRATOR] Iniciando análise de arquivo: {}", file.getOriginalFilename());
        
        final Long userId = 1L; // TODO: Pegar do contexto de segurança real
        UserProfileAI userProfile = memoryService.getProfile(userId);
        log.info("Perfil do usuário carregado: {}", userProfile.getSummary());

        String selectedProviderName = getProviderName(providerName);
        AIProvider selectedProvider = getProvider(selectedProviderName);

        // Cria uma sessão de chat para salvar as mensagens
        ChatSession session = chatService.createDocumentAnalysisSession(file.getOriginalFilename(), userPrompt);
        log.info("Sessão de chat criada: {}", session.getId());
        
        // Salva a mensagem do usuário (prompt + nome do arquivo)
        String userMessageContent = String.format("Arquivo: %s\n\nPrompt: %s", file.getOriginalFilename(), userPrompt);
        ChatMessage userMessage = chatService.saveMessage(session, "user", userMessageContent);
        log.info("Mensagem do usuário salva no banco: {}", userMessage.getId());

        // Constrói o prompt base considerando o perfil do usuário
        PromptPair basePrompts = promptBuilderService.buildGenericPrompt(userPrompt, userProfile, Optional.empty(), Optional.empty());
        log.info("Prompt de sistema base gerado.");

        // --- FLUXO PARA IMAGENS (MULTIMODAL) ---
        if (file.getContentType() != null && file.getContentType().startsWith("image/")) {
            log.info("Tipo de arquivo detectado: IMAGEM. Preparando requisição multimodal.");
            
            AIProviderRequest request = new AIProviderRequest(
                userPrompt, 
                basePrompts.systemPrompt(), 
                null, 
                selectedProviderName, 
                true, 
                file.getBytes(), 
                file.getContentType(), 
                null, 
                null
            );
            return executeAndLogTask(request, selectedProvider, "análise de imagem")
                    .thenCompose(response -> saveResponseAndUpdateProfile(response, session, userMessage, userId));
        
        } else {
            // --- FLUXO PARA DOCUMENTOS DE TEXTO (MAP-REDUCE) ---
            log.info("Tipo de arquivo detectado: TEXTO/DOCUMENTO. Iniciando fluxo Map-Reduce.");

            // 1. Leitura do arquivo usando Apache Tika (extrai texto de PDF, DOCX, etc.)
            TikaDocumentReader documentReader = new TikaDocumentReader(file.getResource());
            List<Document> documents = documentReader.get();
            log.info("Texto extraído com sucesso. Tamanho total aprox: {} bytes.", file.getSize());

            // 2. Chunking (Divisão do texto em pedaços menores)
            // TokenTextSplitter tenta dividir sem quebrar frases no meio
            TokenTextSplitter textSplitter = new TokenTextSplitter(4000, 200, 5, 10000, true);
            List<Document> chunkedDocuments = textSplitter.apply(documents);
            log.info("Documento dividido em {} partes (chunks) para processamento paralelo.", chunkedDocuments.size());

            // 3. Passo MAP: Processa cada pedaço individualmente
            List<CompletableFuture<AIProviderResponse>> mapFutures = chunkedDocuments.stream()
                    .map(chunk -> CompletableFuture.supplyAsync(() -> {
                        try {
                            log.debug("Aguardando permissão do semáforo para processar chunk...");
                            semaphore.acquire(); // Bloqueia se já houver 3 processando
                            log.debug("Permissão concedida. Processando chunk de tamanho: {}", chunk.getContent().length());
                            
                            String mapPrompt = String.format("Analise e resuma esta parte do documento focado em extrair insights, pontos chave e conclusões parciais: \n\n---\n%s\n\n---", chunk.getContent());
                            AIProviderRequest mapRequest = new AIProviderRequest(mapPrompt, basePrompts.systemPrompt(), null, selectedProviderName);
                            
                            // .join() força a espera síncrona dentro da thread do CompletableFuture
                            AIProviderResponse response = selectedProvider.executeTask(mapRequest).join();
                            log.debug("Chunk processado com sucesso.");
                            return response;

                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            log.error("Thread interrompida durante a aquisição do semáforo.", e);
                            throw new RuntimeException(e);
                        } finally {
                            semaphore.release(); // Libera a vaga para o próximo chunk
                            log.debug("Permissão do semáforo liberada.");
                        }
                    }))
                    .collect(Collectors.toList());

            CompletableFuture<Void> allOfMap = CompletableFuture.allOf(mapFutures.toArray(new CompletableFuture[0]));

            // 4. Passo REDUCE: Junta todas as respostas parciais em uma resposta final
            return allOfMap.thenCompose(v -> {
                String combinedPartials = mapFutures.stream()
                        .map(CompletableFuture::join)
                        .map(AIProviderResponse::getContent)
                        .collect(Collectors.joining("\n\n---\n\n"));
                
                log.info("Todas as partes processadas. Tamanho do texto combinado: {} caracteres.", combinedPartials.length());
                log.debug("Conteúdo combinado (início): {}", combinedPartials.substring(0, Math.min(combinedPartials.length(), 200)));

                String reducePrompt = String.format(
                        "Junte as análises parciais a seguir em um relatório final coeso e bem estruturado, respondendo à solicitação original do usuário. Solicitação do usuário: '%s'.\n\n--- ANÁLISES PARCIAIS ---\n%s",
                        userPrompt, combinedPartials
                );

                AIProviderRequest reduceRequest = new AIProviderRequest(reducePrompt, basePrompts.systemPrompt(), null, selectedProviderName);
                return executeAndLogTask(reduceRequest, selectedProvider, "análise final (Reduce)")
                        .thenCompose(response -> saveResponseAndUpdateProfile(response, session, userMessage, userId));
            });
        }
    }


    /**
     * Salva a resposta do assistente no banco e atualiza o perfil do usuário.
     */
    private CompletableFuture<AIProviderResponse> saveResponseAndUpdateProfile(
            AIProviderResponse response, 
            ChatSession session, 
            ChatMessage userMessage, 
            Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Salva a resposta do assistente
                ChatMessage assistantMessage = chatService.saveMessage(session, "assistant", response.getContent());
                log.info("Resposta do assistente salva no banco: {}", assistantMessage.getId());

                // Atualiza o perfil do usuário para RAG
                List<Map<String, String>> chatHistory = new ArrayList<>();
                chatHistory.add(Map.of("role", "user", "content", userMessage.getContent()));
                chatHistory.add(Map.of("role", "assistant", "content", assistantMessage.getContent()));
                memoryService.updateUserProfile(userId, chatHistory);
                log.info("Perfil do usuário atualizado para RAG.");

                return response;
            } catch (Exception e) {
                log.error("Erro ao salvar resposta no banco ou atualizar perfil: {}", e.getMessage(), e);
                // Retorna a resposta mesmo se houver erro ao salvar
                return response;
            }
        });
    }

    /**
     * Método auxiliar para executar a tarefa e logar o resultado ou erro.
     */
    private CompletableFuture<AIProviderResponse> executeAndLogTask(AIProviderRequest request, AIProvider provider, String taskName) {
        log.debug("Enviando requisição '{}' para o provedor '{}'", taskName, provider.getClass().getSimpleName());
        
        return provider.executeTask(request)
                .whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        log.error("!!! ERRO na execução da tarefa '{}': {}", taskName, throwable.getMessage(), throwable);
                    } else {
                        log.info("<<< SUCESSO na tarefa '{}'. Resposta recebida.", taskName);
                        // Loga apenas os primeiros 100 caracteres da resposta para não poluir demais, 
                        // o log completo está no Provider
                        String preview = response.getContent().length() > 100 ? response.getContent().substring(0, 100) + "..." : response.getContent();
                        log.debug("Preview da resposta: {}", preview);
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
}
