package com.matheusdev.mindforge.kanban.mapper;

import com.matheusdev.mindforge.document.mapper.DocumentMapper;
import com.matheusdev.mindforge.kanban.dto.KanbanColumnRequest;
import com.matheusdev.mindforge.kanban.dto.KanbanColumnResponse;
import com.matheusdev.mindforge.kanban.dto.KanbanTaskRequest;
import com.matheusdev.mindforge.kanban.dto.KanbanTaskResponse;
import com.matheusdev.mindforge.kanban.model.KanbanColumn;
import com.matheusdev.mindforge.kanban.model.KanbanTask;
import com.matheusdev.mindforge.project.model.Project;
import com.matheusdev.mindforge.study.subject.model.Subject;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.BeanMapping;

@Mapper(
    componentModel = "spring", 
    uses = {DocumentMapper.class},
    // Ignora campos nulos durante as atualizações para não sobrescrever dados existentes com nulos
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE 
)
public interface KanbanMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "board", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    KanbanColumn toEntity(KanbanColumnRequest request);

    KanbanColumnResponse toResponse(KanbanColumn column);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "documents", ignore = true)
    @Mapping(target = "column", source = "column")
    @Mapping(target = "subject", source = "subject")
    @Mapping(target = "project", source = "project")
    @Mapping(target = "title", source = "request.title")
    @Mapping(target = "description", source = "request.description")
    @Mapping(target = "position", source = "request.position")
    KanbanTask toEntity(KanbanTaskRequest request, KanbanColumn column, Subject subject, Project project);

    @Mapping(source = "column.id", target = "columnId")
    @Mapping(source = "subject.id", target = "subjectId")
    @Mapping(source = "subject.name", target = "subjectName")
    @Mapping(source = "project.id", target = "projectId")
    @Mapping(source = "project.name", target = "projectName")
    KanbanTaskResponse toResponse(KanbanTask task);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "column", ignore = true)
    @Mapping(target = "subject", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "documents", ignore = true)
    void updateTaskFromRequest(KanbanTaskRequest request, @MappingTarget KanbanTask task);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "board", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    void updateColumnFromRequest(KanbanColumnRequest request, @MappingTarget KanbanColumn column);
}
