package com.matheusdev.mindforge.note.mapper;

import com.matheusdev.mindforge.note.dto.NoteRequestDTO;
import com.matheusdev.mindforge.note.dto.NoteResponseDTO;
import com.matheusdev.mindforge.note.model.Note;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface NoteMapper {

    Note toEntity(NoteRequestDTO requestDTO);

    NoteResponseDTO toDTO(Note note);

    void updateEntityFromDTO(NoteRequestDTO requestDTO, @MappingTarget Note note);
}
