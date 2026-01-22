package com.matheusdev.mindforge.ai.chat.model;

import com.matheusdev.mindforge.project.model.Project;
import com.matheusdev.mindforge.study.subject.model.Subject;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private LocalDateTime createdAt;

    @Column(name = "document_id")
    private String documentId;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> messages = new ArrayList<>();

    // --- Context Links ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_item_id")
    private com.matheusdev.mindforge.knowledgeltem.model.KnowledgeItem knowledgeItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_note_id")
    private com.matheusdev.mindforge.study.note.model.Note studyNote;
}
