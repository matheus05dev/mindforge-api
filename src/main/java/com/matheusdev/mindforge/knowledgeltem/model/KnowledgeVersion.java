package com.matheusdev.mindforge.knowledgeltem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_versions", indexes = {
        @Index(name = "idx_knowledge_item_id", columnList = "knowledge_item_id"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "knowledge_item_id", nullable = false)
    private Long knowledgeItemId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 50)
    private ChangeType changeType;

    @Column(name = "proposal_id", length = 100)
    private String proposalId;

    @Column(name = "change_summary", length = 500)
    private String changeSummary;

    public enum ChangeType {
        MANUAL_EDIT,
        AGENT_PROPOSAL,
        ROLLBACK,
        INITIAL_VERSION
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
