package com.matheusdev.mindforge.study.note.repository;

import com.matheusdev.mindforge.study.note.model.StudyNoteVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyNoteVersionRepository extends JpaRepository<StudyNoteVersion, Long> {

    List<StudyNoteVersion> findByStudyNoteIdOrderByCreatedAtDesc(Long studyNoteId);

    void deleteByStudyNoteId(Long studyNoteId);
}
