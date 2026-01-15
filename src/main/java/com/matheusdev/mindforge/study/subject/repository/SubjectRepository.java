package com.matheusdev.mindforge.study.subject.repository;

import com.matheusdev.mindforge.study.subject.model.Subject;
import com.matheusdev.mindforge.study.subject.model.enums.ProficiencyLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByProficiencyLevel(ProficiencyLevel level);
    List<Subject> findByWorkspaceId(Long workspaceId); // Nova query
}