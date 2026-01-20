package com.matheusdev.mindforge.knowledgeltem.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChatSessionResponse {
    private Long id; // Session ID
    private List<MessageResponse> messages;

    @Data
    public static class MessageResponse {
        private Long id;
        private String role; // "user" or "assistant"
        private String content;
        private LocalDateTime createdAt;
    }
}
