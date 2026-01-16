package com.matheusdev.mindforge.study.repository;

import com.matheusdev.mindforge.study.model.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudySessionRepository extends JpaRepository<StudySession, Long> {
    List<StudySession> findBySubjectId(Long subjectId);
}
