package com.matheusdev.mindforge.study.resource.repository;

import com.matheusdev.mindforge.study.resource.model.StudyResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyResourceRepository extends JpaRepository<StudyResource, Long> {
    List<StudyResource> findBySubjectId(Long subjectId);
}
