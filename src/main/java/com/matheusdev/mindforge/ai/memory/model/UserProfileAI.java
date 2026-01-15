package com.matheusdev.mindforge.ai.memory.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class UserProfileAI {

    @Id
    // No futuro, este ID deve ser o mesmo do usuário (ex: @MapsId)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String summary; // Resumo em linguagem natural do perfil do usuário

    @Column(columnDefinition = "TEXT")
    private String structuredProfile; // JSON com dados estruturados (pontos fortes, fracos, etc.)

    @Enumerated(EnumType.STRING)
    private LearningStyle learningStyle = LearningStyle.PRACTICAL; // Default

    @Enumerated(EnumType.STRING)
    private CommunicationTone communicationTone = CommunicationTone.ENCOURAGING; // Default

    private String preferredModel; // Ex: "gpt-4", "claude-3-opus", ou null para automático

    private LocalDateTime lastUpdatedAt;

    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        lastUpdatedAt = LocalDateTime.now();
    }
}
