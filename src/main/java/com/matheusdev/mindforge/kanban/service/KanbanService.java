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
        Long tenantId = com.matheusdev.mindforge.core.tenant.context.TenantContext.getTenantId();
        return columnRepository.findAllByTenantId(tenantId).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    public KanbanColumnResponse createColumn(KanbanColumnRequest request) {
        // Needs to link to a board from this tenant.
        // Current implementation seems to assume global columns or one board.
        // We will need to fetch or create a board for the tenant.
        // For now, assume mapper sets up minimal entity and we save.
        // This is a temporary measure until Board Management is fully explicit.

        KanbanColumn column = mapper.toEntity(request);
        return mapper.toResponse(columnRepository.save(column));
    }

    @Transactional
    public KanbanTaskResponse createTask(Long columnId, KanbanTaskRequest request) {
        Long tenantId = com.matheusdev.mindforge.core.tenant.context.TenantContext.getTenantId();
        KanbanColumn column = columnRepository.findByIdAndTenantId(columnId, tenantId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Coluna do Kanban não encontrada com o id: " + columnId));

        Subject subject = null;
        if (request.getSubjectId() != null) {
            subject = subjectRepository.findByIdAndTenantId(request.getSubjectId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Assunto de estudo não encontrado com o id: " + request.getSubjectId()));
        }

        Project project = null;
        if (request.getProjectId() != null) {
            project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Projeto não encontrado com o id: " + request.getProjectId()));
            if (!project.getTenant().getId().equals(tenantId)) {
                throw new ResourceNotFoundException("Projeto não encontrado");
            }
        }

        KanbanTask task = mapper.toEntity(request, column, subject, project);
        return mapper.toResponse(taskRepository.save(task));
    }

    @Transactional
    public KanbanTaskResponse moveTask(Long taskId, Long targetColumnId) {
        Long tenantId = com.matheusdev.mindforge.core.tenant.context.TenantContext.getTenantId();

        KanbanTask task = taskRepository.findById(taskId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Tarefa do Kanban não encontrada com o id: " + taskId));

        // Use getTenantId() to avoid compilation error or lazy loading issues
        if (!task.getColumn().getBoard().getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Tarefa não encontrada");
        }

        KanbanColumn targetColumn = columnRepository.findByIdAndTenantId(targetColumnId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Coluna de destino do Kanban não encontrada com o id: " + targetColumnId));

        task.setColumn(targetColumn);
        return mapper.toResponse(taskRepository.save(task));
    }

    @Transactional
    public KanbanTaskResponse updateTask(Long taskId, KanbanTaskRequest request) {
        Long tenantId = com.matheusdev.mindforge.core.tenant.context.TenantContext.getTenantId();
        KanbanTask task = taskRepository.findById(taskId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Tarefa do Kanban não encontrada com o id: " + taskId));

        if (!task.getColumn().getBoard().getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Tarefa não encontrada");
        }

        mapper.updateTaskFromRequest(request, task);
        return mapper.toResponse(taskRepository.save(task));
    }

    public void deleteTask(Long taskId) {
        Long tenantId = com.matheusdev.mindforge.core.tenant.context.TenantContext.getTenantId();
        KanbanTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada"));

        if (!task.getColumn().getBoard().getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Tarefa não encontrada");
        }
        taskRepository.deleteById(taskId);
    }

    @Transactional
    public KanbanColumnResponse updateColumn(Long columnId, KanbanColumnRequest request) {
        Long tenantId = com.matheusdev.mindforge.core.tenant.context.TenantContext.getTenantId();
        KanbanColumn column = columnRepository.findByIdAndTenantId(columnId, tenantId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Coluna do Kanban não encontrada com o id: " + columnId));

        mapper.updateColumnFromRequest(request, column);
        return mapper.toResponse(columnRepository.save(column));
    }

    public void deleteColumn(Long columnId) {
        Long tenantId = com.matheusdev.mindforge.core.tenant.context.TenantContext.getTenantId();
        KanbanColumn column = columnRepository.findByIdAndTenantId(columnId, tenantId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Coluna do Kanban não encontrada com o id: " + columnId));
        columnRepository.delete(column);
    }

    public List<KanbanColumnResponse> getAllColumns() {
        Long tenantId = com.matheusdev.mindforge.core.tenant.context.TenantContext.getTenantId();
        return columnRepository.findAllByTenantId(tenantId).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    public KanbanColumnResponse getColumnById(Long columnId) {
        Long tenantId = com.matheusdev.mindforge.core.tenant.context.TenantContext.getTenantId();
        KanbanColumn column = columnRepository.findByIdAndTenantId(columnId, tenantId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Coluna do Kanban não encontrada com o id: " + columnId));
        return mapper.toResponse(column);
    }

    public List<KanbanTaskResponse> getTasksByColumn(Long columnId) {
        Long tenantId = com.matheusdev.mindforge.core.tenant.context.TenantContext.getTenantId();
        KanbanColumn column = columnRepository.findByIdAndTenantId(columnId, tenantId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Coluna do Kanban não encontrada com o id: " + columnId));

        return taskRepository.findByColumnId(columnId).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    public KanbanTaskResponse getTaskById(Long taskId) {
        Long tenantId = com.matheusdev.mindforge.core.tenant.context.TenantContext.getTenantId();
        KanbanTask task = taskRepository.findById(taskId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Tarefa do Kanban não encontrada com o id: " + taskId));

        if (!task.getColumn().getBoard().getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Tarefa não encontrada");
        }
        return mapper.toResponse(task);
    }
}
