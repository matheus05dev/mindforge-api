package com.matheusdev.mindforge.study.mapper;

import com.matheusdev.mindforge.document.mapper.DocumentMapper;
import com.matheusdev.mindforge.study.dto.StudySessionRequest;
import com.matheusdev.mindforge.study.dto.StudySessionResponse;
import com.matheusdev.mindforge.study.model.StudySession;
import com.matheusdev.mindforge.study.subject.dto.SubjectRequest;
import com.matheusdev.mindforge.study.subject.dto.SubjectResponse;
import com.matheusdev.mindforge.study.subject.model.Subject;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = {DocumentMapper.class})
public interface StudyMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "workspace", ignore = true)
    @Mapping(target = "studySessions", ignore = true)
    Subject toEntity(SubjectRequest request);

    SubjectResponse toResponse(Subject subject);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subject", ignore = true)
    @Mapping(target = "documents", ignore = true)
    StudySession toEntity(StudySessionRequest request);

    @Mapping(source = "subject.id", target = "subjectId")
    @Mapping(source = "subject.name", target = "subjectName")
    StudySessionResponse toResponse(StudySession session);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "workspace", ignore = true)
    @Mapping(target = "studySessions", ignore = true)
    void updateSubjectFromRequest(SubjectRequest request, @MappingTarget Subject subject);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subject", ignore = true)
    @Mapping(target = "documents", ignore = true)
    void updateSessionFromRequest(StudySessionRequest request, @MappingTarget StudySession session);
}
