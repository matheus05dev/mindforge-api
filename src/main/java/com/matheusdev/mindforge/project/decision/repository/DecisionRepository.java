package com.matheusdev.mindforge.project.decision.repository;

import com.matheusdev.mindforge.project.decision.model.DecisionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DecisionRepository extends JpaRepository<DecisionRecord, Long> {
    List<DecisionRecord> findByProjectId(Long projectId);

    List<DecisionRecord> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}
