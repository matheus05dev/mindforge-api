package com.matheusdev.mindforge.study.note.mapper;

import com.matheusdev.mindforge.study.note.dto.NoteRequest;
import com.matheusdev.mindforge.study.note.dto.NoteResponse;
import com.matheusdev.mindforge.study.note.model.Note;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface StudyNoteMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subject", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Note toEntity(NoteRequest request);

    @Mapping(source = "subject.id", target = "subjectId")
    NoteResponse toResponse(Note note);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subject", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateNoteFromRequest(NoteRequest request, @MappingTarget Note note);
}
