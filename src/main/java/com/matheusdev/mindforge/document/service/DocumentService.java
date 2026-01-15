package com.matheusdev.mindforge.document.service;

import com.matheusdev.mindforge.document.model.Document;
import com.matheusdev.mindforge.document.repository.DocumentRepository;
import com.matheusdev.mindforge.kanban.model.KanbanTask;
import com.matheusdev.mindforge.kanban.repository.KanbanTaskRepository;
import com.matheusdev.mindforge.knowledgeltem.model.KnowledgeItem;
import com.matheusdev.mindforge.knowledgeltem.repository.KnowledgeItemRepository;
import com.matheusdev.mindforge.project.model.Project;
import com.matheusdev.mindforge.project.repository.ProjectRepository;
import com.matheusdev.mindforge.study.model.StudySession;
import com.matheusdev.mindforge.study.repository.StudySessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final ProjectRepository projectRepository;
    private final KanbanTaskRepository kanbanTaskRepository;
    private final KnowledgeItemRepository knowledgeItemRepository;
    private final StudySessionRepository studySessionRepository;

    public Document storeFile(MultipartFile file, Long projectId, Long kanbanTaskId, Long knowledgeItemId, Long studySessionId) {
        String fileName = fileStorageService.storeFile(file);

        Document document = new Document();
        document.setFileName(fileName);
        document.setFileType(file.getContentType());
        document.setFilePath("/uploads/" + fileName);
        document.setUploadDate(LocalDateTime.now());

        if (projectId != null) {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));
            document.setProject(project);
        } else if (kanbanTaskId != null) {
            KanbanTask kanbanTask = kanbanTaskRepository.findById(kanbanTaskId)
                    .orElseThrow(() -> new RuntimeException("Kanban task not found"));
            document.setKanbanTask(kanbanTask);
        } else if (knowledgeItemId != null) {
            KnowledgeItem knowledgeItem = knowledgeItemRepository.findById(knowledgeItemId)
                    .orElseThrow(() -> new RuntimeException("Knowledge item not found"));
            document.setKnowledgeItem(knowledgeItem);
        } else if (studySessionId != null) {
            StudySession studySession = studySessionRepository.findById(studySessionId)
                    .orElseThrow(() -> new RuntimeException("Study session not found"));
            document.setStudySession(studySession);
        }

        return documentRepository.save(document);
    }
}
