package com.matheusdev.mindforge.document.model;

import com.matheusdev.mindforge.kanban.model.KanbanTask;
import com.matheusdev.mindforge.knowledgeltem.model.KnowledgeItem;
import com.matheusdev.mindforge.project.model.Project;
import com.matheusdev.mindforge.study.model.StudySession;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = true)
    private Long workspaceId; // Pode ser nulo se for um documento global

    private String fileName;
    private String fileType;
    private String filePath;
    private LocalDateTime uploadDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = true)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kanban_task_id", nullable = true)
    private KanbanTask kanbanTask;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_item_id", nullable = true)
    private KnowledgeItem knowledgeItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_session_id", nullable = true)
    private StudySession studySession;
}
