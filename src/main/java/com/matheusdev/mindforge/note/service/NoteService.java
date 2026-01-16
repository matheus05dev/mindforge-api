package com.matheusdev.mindforge.note.service;

import com.matheusdev.mindforge.note.dto.NoteRequestDTO;
import com.matheusdev.mindforge.note.dto.NoteResponseDTO;
import com.matheusdev.mindforge.note.mapper.NoteMapper;
import com.matheusdev.mindforge.note.model.Note;
import com.matheusdev.mindforge.note.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final NoteMapper noteMapper;

    public NoteResponseDTO createNote(NoteRequestDTO requestDTO) {
        Note note = noteMapper.toEntity(requestDTO);
        Note savedNote = noteRepository.save(note);
        return noteMapper.toDTO(savedNote);
    }

    public List<NoteResponseDTO> getAllNotes() {
        return noteRepository.findAll().stream()
                .map(noteMapper::toDTO)
                .collect(Collectors.toList());
    }

    public NoteResponseDTO getNoteById(Long id) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nota não encontrada"));
        return noteMapper.toDTO(note);
    }

    public NoteResponseDTO updateNote(Long id, NoteRequestDTO requestDTO) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nota não encontrada"));
        noteMapper.updateEntityFromDTO(requestDTO, note);
        Note updatedNote = noteRepository.save(note);
        return noteMapper.toDTO(updatedNote);
    }

    public void deleteNote(Long id) {
        noteRepository.deleteById(id);
    }
}
