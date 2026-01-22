package com.matheusdev.mindforge.ai.chat.repository;

import com.matheusdev.mindforge.ai.chat.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findByProjectId(Long projectId);

    Optional<ChatSession> findByDocumentId(String documentId);

    Optional<ChatSession> findByKnowledgeItemId(Long knowledgeItemId);

    Optional<ChatSession> findByStudyNoteId(Long studyNoteId);
}
