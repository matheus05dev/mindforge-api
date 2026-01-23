package com.matheusdev.mindforge.ai.service.chain;

import com.matheusdev.mindforge.ai.chat.model.ChatMessage;
import com.matheusdev.mindforge.ai.chat.model.ChatSession;
import com.matheusdev.mindforge.ai.chat.repository.ChatSessionRepository;
import com.matheusdev.mindforge.ai.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class ValidationStep implements AIProcessingStep {

    private final ChatService chatService;
    private static final long MAX_VALID_SESSION_ID = 1_000_000_000L;

    @Override
    public CompletableFuture<AIContext> execute(AIContext context) {
        return CompletableFuture.supplyAsync(() -> {
            log.info(">> [CHAIN] Step 1: Validation");

            ChatSession session = ensureSession(context.getRequest().chatId());

            String userPrompt = (context.getRequest().prompt() == null || context.getRequest().prompt().isBlank())
                    ? "(Interação iniciada sem prompt)"
                    : context.getRequest().prompt();

            if (context.getRequest().prompt() == null || context.getRequest().prompt().isBlank()) {
                log.warn("Prompt vazio para sessão {}.", session.getId());
            }

            // Save User Message immediately
            ChatMessage userMessage = chatService.saveMessage(session, "user", userPrompt);
            log.info("Mensagem do usuário salva no banco: {}", userMessage.getId());

            // Context Correction Logic (moved from Orchestrator)
            if (!StringUtils.hasText(session.getDocumentId())
                    && StringUtils.hasText(context.getRequest().documentId())) {
                log.warn(
                        "⚠️ CORREÇÃO DE CONTEXTO: Sessão {} estava sem documentId, mas request informou '{}'. Atualizando...",
                        session.getId(), context.getRequest().documentId());
                session.setDocumentId(context.getRequest().documentId());
                chatService.updateSession(session);
            }

            // Return updated context with Session and Message
            return context.withSession(session).withUserMessage(userMessage);
        });
    }

    private ChatSession ensureSession(Long chatId) {
        if (chatId == null || chatId > MAX_VALID_SESSION_ID) {
            return chatService.createEmergencySession();
        }
        return chatService.getSession(chatId).orElseGet(chatService::createEmergencySession);
    }
}
