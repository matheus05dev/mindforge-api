package com.matheusdev.mindforge.kanban.service;

import com.matheusdev.mindforge.kanban.dto.KanbanColumnRequest;
import com.matheusdev.mindforge.kanban.dto.KanbanColumnResponse;
import com.matheusdev.mindforge.kanban.dto.KanbanTaskRequest;
import com.matheusdev.mindforge.kanban.dto.KanbanTaskResponse;
import com.matheusdev.mindforge.kanban.mapper.KanbanMapper;
import com.matheusdev.mindforge.kanban.model.KanbanColumn;
import com.matheusdev.mindforge.kanban.model.KanbanTask;
import com.matheusdev.mindforge.kanban.repository.KanbanColumnRepository;
import com.matheusdev.mindforge.kanban.repository.KanbanTaskRepository;
import com.matheusdev.mindforge.project.model.Project;
import com.matheusdev.mindforge.project.repository.ProjectRepository;
import com.matheusdev.mindforge.study.subject.model.Subject; // Corrected import
import com.matheusdev.mindforge.study.subject.repository.SubjectRepository; // Corrected import
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KanbanService {

    private final KanbanColumnRepository columnRepository;
    private final KanbanTaskRepository taskRepository;
    private final SubjectRepository subjectRepository;
    private final ProjectRepository projectRepository;
    private final KanbanMapper mapper;

    public List<KanbanColumnResponse> getBoard() {
        return columnRepository.findAll().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    public KanbanColumnResponse createColumn(KanbanColumnRequest request) {
        KanbanColumn column = mapper.toEntity(request);
        return mapper.toResponse(columnRepository.save(column));
    }

    @Transactional
    public KanbanTaskResponse createTask(Long columnId, KanbanTaskRequest request) {
        KanbanColumn column = columnRepository.findById(columnId)
                .orElseThrow(() -> new RuntimeException("Column not found"));

        Subject subject = null;
        if (request.getSubjectId() != null) {
            subject = subjectRepository.findById(request.getSubjectId())
                    .orElseThrow(() -> new RuntimeException("Subject not found"));
        }

        Project project = null;
        if (request.getProjectId() != null) {
            project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found"));
        }

        KanbanTask task = mapper.toEntity(request, column, subject, project);
        return mapper.toResponse(taskRepository.save(task));
    }

    @Transactional
    public KanbanTaskResponse moveTask(Long taskId, Long targetColumnId) {
        KanbanTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        KanbanColumn targetColumn = columnRepository.findById(targetColumnId)
                .orElseThrow(() -> new RuntimeException("Target column not found"));
        
        task.setColumn(targetColumn);
        return mapper.toResponse(taskRepository.save(task));
    }

    @Transactional
    public KanbanTaskResponse updateTask(Long taskId, KanbanTaskRequest request) {
        KanbanTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        mapper.updateTaskFromRequest(request, task);
        return mapper.toResponse(taskRepository.save(task));
    }

    public void deleteTask(Long taskId) {
        taskRepository.deleteById(taskId);
    }

    @Transactional
    public KanbanColumnResponse updateColumn(Long columnId, KanbanColumnRequest request) {
        KanbanColumn column = columnRepository.findById(columnId)
                .orElseThrow(() -> new RuntimeException("Column not found"));
        
        mapper.updateColumnFromRequest(request, column);
        return mapper.toResponse(columnRepository.save(column));
    }

    public void deleteColumn(Long columnId) {
        columnRepository.deleteById(columnId);
    }
}
