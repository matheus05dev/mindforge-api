package com.matheusdev.mindforge.workspace.mapper;

import com.matheusdev.mindforge.workspace.dto.WorkspaceRequest;
import com.matheusdev.mindforge.workspace.dto.WorkspaceResponse;
import com.matheusdev.mindforge.workspace.model.Workspace;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WorkspaceMapper {

    @Mapping(target = "id", ignore = true)
    Workspace toEntity(WorkspaceRequest request);
    
    WorkspaceResponse toResponse(Workspace workspace);
}
