package com.matheusdev.mindforge.ai.service;

import com.matheusdev.mindforge.ai.chat.model.ChatMessage;
import com.matheusdev.mindforge.ai.chat.model.ChatSession;
import com.matheusdev.mindforge.ai.chat.repository.ChatMessageRepository;
import com.matheusdev.mindforge.ai.chat.repository.ChatSessionRepository;
import com.matheusdev.mindforge.ai.dto.GenericAnalysisRequest;
import com.matheusdev.mindforge.project.repository.ProjectRepository;
import com.matheusdev.mindforge.study.subject.model.Subject;
import com.matheusdev.mindforge.study.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SubjectRepository subjectRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public Optional<ChatSession> getSession(Long chatId) {
        return chatSessionRepository.findById(chatId);
    }

    @Transactional(readOnly = true)
    public java.util.List<ChatSession> getAllSessions() {
        return chatSessionRepository.findAll(org.springframework.data.domain.Sort
                .by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    @Transactional
    public ChatSession createEmergencySession() {
        ChatSession session = new ChatSession();
        session.setTitle("Sessão de Emergência (ID Inválido)");
        session.setCreatedAt(LocalDateTime.now());
        return chatSessionRepository.save(session);
    }

    @Transactional
    public ChatSession createNewSession() {
        ChatSession session = new ChatSession();
        session.setTitle("Nova Conversa");
        session.setCreatedAt(LocalDateTime.now());
        return chatSessionRepository.save(session);
    }

    @Transactional
    public ChatSession updateSession(ChatSession session) {
        return chatSessionRepository.save(session);
    }

    @Transactional
    public Optional<ChatSession> updateSessionTitle(Long sessionId, String newTitle) {
        return chatSessionRepository.findById(sessionId)
                .map(session -> {
                    session.setTitle(newTitle);
                    return chatSessionRepository.save(session);
                });
    }

    @Transactional
    public boolean deleteSession(Long sessionId) {
        if (chatSessionRepository.existsById(sessionId)) {
            chatSessionRepository.deleteById(sessionId);
            return true;
        }
        return false;
    }

    @Transactional
    public ChatSession getOrCreateChatSession(Subject subject, String mode) {
        ChatSession session = new ChatSession();
        session.setSubject(subject);
        session.setTitle(String.format("Análise de Código (%s) para %s", mode, subject.getName()));
        session.setCreatedAt(LocalDateTime.now());
        return chatSessionRepository.save(session);
    }

    @Transactional
    public ChatSession getOrCreateGenericChatSession(
            GenericAnalysisRequest request) {
        ChatSession session = new ChatSession();
        if (request != null && request.getSubjectId() != null) {
            session.setSubject(subjectRepository.findById(request.getSubjectId()).orElse(null));
        }
        if (request != null && request.getProjectId() != null) {
            session.setProject(projectRepository.findById(request.getProjectId()).orElse(null));
        }
        String title = (request != null) ? request.getQuestion() : "Sessão Genérica";
        session.setTitle("Análise: " + title.substring(0, Math.min(title.length(), 20)) + "...");
        session.setCreatedAt(LocalDateTime.now());
        return chatSessionRepository.save(session);
    }

    @Transactional
    public ChatMessage saveMessage(ChatSession session, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setSession(session);
        message.setRole(role);
        // Fallback de segurança para garantir que o conteúdo nunca seja nulo
        message.setContent(StringUtils.hasText(content) ? content : "(Conteúdo vazio)");
        return chatMessageRepository.save(message);
    }

    @Transactional
    public ChatSession createDocumentAnalysisSession(String fileName,
            String userPrompt) {
        ChatSession session = new ChatSession();
        String title = String.format("Análise de Documento: %s", fileName);
        if (userPrompt != null && userPrompt.length() > 0) {
            String promptPreview = userPrompt.substring(0, Math.min(userPrompt.length(), 30));
            title = String.format("Análise: %s - %s...", fileName, promptPreview);
        }
        session.setTitle(title);
        session.setCreatedAt(LocalDateTime.now());
        session.setDocumentId(fileName); // Salva o nome do arquivo como documentId
        return chatSessionRepository.save(session);
    }

    public com.matheusdev.mindforge.knowledgeltem.dto.ChatSessionResponse mapToResponse(ChatSession session) {
        com.matheusdev.mindforge.knowledgeltem.dto.ChatSessionResponse response = new com.matheusdev.mindforge.knowledgeltem.dto.ChatSessionResponse();
        response.setId(session.getId());
        response.setTitle(session.getTitle());
        response.setDocumentId(session.getDocumentId());

        if (session.getMessages() != null) {
            response.setMessages(session.getMessages().stream().map(msg -> {
                com.matheusdev.mindforge.knowledgeltem.dto.ChatSessionResponse.MessageResponse msgResponse = new com.matheusdev.mindforge.knowledgeltem.dto.ChatSessionResponse.MessageResponse();
                msgResponse.setId(msg.getId());
                msgResponse.setRole(msg.getRole());
                msgResponse.setContent(msg.getContent());
                msgResponse.setCreatedAt(msg.getCreatedAt());
                return msgResponse;
            }).collect(java.util.stream.Collectors.toList()));
        }

        return response;
    }
}
