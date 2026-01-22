package com.matheusdev.mindforge.study.note.service;

import com.matheusdev.mindforge.exception.ResourceNotFoundException;
import com.matheusdev.mindforge.study.note.dto.NoteRequest;
import com.matheusdev.mindforge.study.note.dto.NoteResponse;
import com.matheusdev.mindforge.study.note.mapper.StudyNoteMapper;
import com.matheusdev.mindforge.study.note.model.Note;
import com.matheusdev.mindforge.study.note.repository.StudyNoteRepository;
import com.matheusdev.mindforge.study.subject.model.Subject;
import com.matheusdev.mindforge.study.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyNoteService {

    private final StudyNoteRepository noteRepository;
    private final SubjectRepository subjectRepository;
    private final StudyNoteMapper mapper;

    public List<NoteResponse> getNotesBySubject(Long subjectId) {
        return noteRepository.findBySubjectIdOrderByUpdatedAtDesc(subjectId).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    public NoteResponse getNoteById(Long noteId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Nota não encontrada com o id: " + noteId));
        return mapper.toResponse(note);
    }

    @Transactional
    public NoteResponse createNote(Long subjectId, NoteRequest request) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Matéria não encontrada com o id: " + subjectId));

        Note note = mapper.toEntity(request);
        note.setSubject(subject);

        Note savedNote = noteRepository.save(note);
        return mapper.toResponse(savedNote);
    }

    @Transactional
    public NoteResponse updateNote(Long noteId, NoteRequest request) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Nota não encontrada com o id: " + noteId));

        mapper.updateNoteFromRequest(request, note);
        Note updatedNote = noteRepository.save(note);
        return mapper.toResponse(updatedNote);
    }

    public void deleteNote(Long noteId) {
        if (!noteRepository.existsById(noteId)) {
            throw new ResourceNotFoundException("Nota não encontrada com o id: " + noteId);
        }
        noteRepository.deleteById(noteId);
    }
}
