package com.matheusdev.mindforge.document.service;

import com.matheusdev.mindforge.document.model.Document;
import com.matheusdev.mindforge.document.repository.DocumentRepository;
import com.matheusdev.mindforge.exception.ResourceNotFoundException;
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
                    .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado com o id: " + projectId));
            document.setProject(project);
        } else if (kanbanTaskId != null) {
            KanbanTask kanbanTask = kanbanTaskRepository.findById(kanbanTaskId)
                    .orElseThrow(() -> new ResourceNotFoundException("Tarefa do Kanban não encontrada com o id: " + kanbanTaskId));
            document.setKanbanTask(kanbanTask);
        } else if (knowledgeItemId != null) {
            KnowledgeItem knowledgeItem = knowledgeItemRepository.findById(knowledgeItemId)
                    .orElseThrow(() -> new ResourceNotFoundException("Item de conhecimento não encontrado com o id: " + knowledgeItemId));
            document.setKnowledgeItem(knowledgeItem);
        } else if (studySessionId != null) {
            StudySession studySession = studySessionRepository.findById(studySessionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sessão de estudo não encontrada com o id: " + studySessionId));
            document.setStudySession(studySession);
        }

        return documentRepository.save(document);
    }
}
