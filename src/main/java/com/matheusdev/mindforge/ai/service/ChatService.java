package com.matheusdev.mindforge.ai.service;

import com.matheusdev.mindforge.ai.chat.model.ChatMessage;
import com.matheusdev.mindforge.ai.chat.model.ChatSession;
import com.matheusdev.mindforge.ai.chat.repository.ChatMessageRepository;
import com.matheusdev.mindforge.ai.chat.repository.ChatSessionRepository;
import com.matheusdev.mindforge.ai.dto.GenericAnalysisRequest;
import com.matheusdev.mindforge.project.model.Project;
import com.matheusdev.mindforge.project.repository.ProjectRepository;
import com.matheusdev.mindforge.study.subject.model.Subject;
import com.matheusdev.mindforge.study.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SubjectRepository subjectRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    public ChatSession getOrCreateChatSession(Subject subject, String mode) {
        ChatSession session = new ChatSession();
        session.setSubject(subject);
        session.setTitle(String.format("Análise de Código (%s) para %s", mode, subject.getName()));
        session.setCreatedAt(LocalDateTime.now());
        return chatSessionRepository.save(session);
    }

    @Transactional
    public ChatSession getOrCreateGenericChatSession(GenericAnalysisRequest request) {
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
        message.setContent(content);
        return chatMessageRepository.save(message);
    }

    @Transactional
    public ChatSession createDocumentAnalysisSession(String fileName, String userPrompt) {
        ChatSession session = new ChatSession();
        String title = String.format("Análise de Documento: %s", fileName);
        if (userPrompt != null && userPrompt.length() > 0) {
            String promptPreview = userPrompt.substring(0, Math.min(userPrompt.length(), 30));
            title = String.format("Análise: %s - %s...", fileName, promptPreview);
        }
        session.setTitle(title);
        session.setCreatedAt(LocalDateTime.now());
        return chatSessionRepository.save(session);
    }
}
