package com.matheusdev.mindforge.ai.chat.repository;

import com.matheusdev.mindforge.ai.chat.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
}
