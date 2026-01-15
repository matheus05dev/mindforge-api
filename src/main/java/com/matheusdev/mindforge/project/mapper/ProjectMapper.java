package com.matheusdev.mindforge.project.mapper;

import com.matheusdev.mindforge.document.mapper.DocumentMapper;
import com.matheusdev.mindforge.project.dto.ProjectRequest;
import com.matheusdev.mindforge.project.dto.ProjectResponse;
import com.matheusdev.mindforge.project.milestone.dto.MilestoneRequest;
import com.matheusdev.mindforge.project.milestone.dto.MilestoneResponse;
import com.matheusdev.mindforge.project.milestone.model.Milestone;
import com.matheusdev.mindforge.project.model.Project;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = {DocumentMapper.class})
public interface ProjectMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "workspace", ignore = true)
    @Mapping(target = "githubRepoUrl", ignore = true)
    @Mapping(target = "milestones", ignore = true)
    @Mapping(target = "documents", ignore = true)
    Project toEntity(ProjectRequest request);

    ProjectResponse toResponse(Project project);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    Milestone toEntity(MilestoneRequest request);

    @Mapping(source = "project.id", target = "projectId")
    MilestoneResponse toResponse(Milestone milestone);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "workspace", ignore = true)
    @Mapping(target = "githubRepoUrl", ignore = true)
    @Mapping(target = "milestones", ignore = true)
    @Mapping(target = "documents", ignore = true)
    void updateProjectFromRequest(ProjectRequest request, @MappingTarget Project project);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    void updateMilestoneFromRequest(MilestoneRequest request, @MappingTarget Milestone milestone);
}
