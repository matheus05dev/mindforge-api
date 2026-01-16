package com.matheusdev.mindforge.kanban.service;

import com.matheusdev.mindforge.exception.ResourceNotFoundException;
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
import com.matheusdev.mindforge.study.subject.model.Subject;
import com.matheusdev.mindforge.study.subject.repository.SubjectRepository;
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
                .orElseThrow(() -> new ResourceNotFoundException("Coluna do Kanban não encontrada com o id: " + columnId));

        Subject subject = null;
        if (request.getSubjectId() != null) {
            subject = subjectRepository.findById(request.getSubjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assunto de estudo não encontrado com o id: " + request.getSubjectId()));
        }

        Project project = null;
        if (request.getProjectId() != null) {
            project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado com o id: " + request.getProjectId()));
        }

        KanbanTask task = mapper.toEntity(request, column, subject, project);
        return mapper.toResponse(taskRepository.save(task));
    }

    @Transactional
    public KanbanTaskResponse moveTask(Long taskId, Long targetColumnId) {
        KanbanTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa do Kanban não encontrada com o id: " + taskId));
        KanbanColumn targetColumn = columnRepository.findById(targetColumnId)
                .orElseThrow(() -> new ResourceNotFoundException("Coluna de destino do Kanban não encontrada com o id: " + targetColumnId));
        
        task.setColumn(targetColumn);
        return mapper.toResponse(taskRepository.save(task));
    }

    @Transactional
    public KanbanTaskResponse updateTask(Long taskId, KanbanTaskRequest request) {
        KanbanTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa do Kanban não encontrada com o id: " + taskId));
        
        mapper.updateTaskFromRequest(request, task);
        return mapper.toResponse(taskRepository.save(task));
    }

    public void deleteTask(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new ResourceNotFoundException("Tarefa do Kanban não encontrada com o id: " + taskId);
        }
        taskRepository.deleteById(taskId);
    }

    @Transactional
    public KanbanColumnResponse updateColumn(Long columnId, KanbanColumnRequest request) {
        KanbanColumn column = columnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("Coluna do Kanban não encontrada com o id: " + columnId));
        
        mapper.updateColumnFromRequest(request, column);
        return mapper.toResponse(columnRepository.save(column));
    }

    public void deleteColumn(Long columnId) {
        if (!columnRepository.existsById(columnId)) {
            throw new ResourceNotFoundException("Coluna do Kanban não encontrada com o id: " + columnId);
        }
        columnRepository.deleteById(columnId);
    }

    public List<KanbanColumnResponse> getAllColumns() {
        return columnRepository.findAll().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    public KanbanColumnResponse getColumnById(Long columnId) {
        KanbanColumn column = columnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("Coluna do Kanban não encontrada com o id: " + columnId));
        return mapper.toResponse(column);
    }

    public List<KanbanTaskResponse> getTasksByColumn(Long columnId) {
        KanbanColumn column = columnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("Coluna do Kanban não encontrada com o id: " + columnId));
        
        return taskRepository.findByColumnId(columnId).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    public KanbanTaskResponse getTaskById(Long taskId) {
        KanbanTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa do Kanban não encontrada com o id: " + taskId));
        return mapper.toResponse(task);
    }
}
