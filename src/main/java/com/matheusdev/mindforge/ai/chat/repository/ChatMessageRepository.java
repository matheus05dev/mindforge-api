package com.matheusdev.mindforge.ai.chat.repository;

import com.matheusdev.mindforge.ai.chat.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
}
