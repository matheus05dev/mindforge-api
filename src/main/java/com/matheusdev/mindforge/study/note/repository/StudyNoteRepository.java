package com.matheusdev.mindforge.study.note.repository;

import com.matheusdev.mindforge.study.note.model.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyNoteRepository extends JpaRepository<Note, Long> {
    List<Note> findBySubjectId(Long subjectId);

    List<Note> findBySubjectIdOrderByUpdatedAtDesc(Long subjectId);
}
