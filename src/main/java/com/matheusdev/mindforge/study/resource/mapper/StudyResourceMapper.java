package com.matheusdev.mindforge.study.resource.mapper;

import com.matheusdev.mindforge.study.resource.dto.StudyResourceRequest;
import com.matheusdev.mindforge.study.resource.dto.StudyResourceResponse;
import com.matheusdev.mindforge.study.resource.model.StudyResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StudyResourceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subject", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    StudyResource toEntity(StudyResourceRequest request);

    @Mapping(source = "subject.id", target = "subjectId")
    StudyResourceResponse toResponse(StudyResource resource);
}
