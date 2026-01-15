package com.matheusdev.mindforge.project.milestone.repository;

import com.matheusdev.mindforge.project.milestone.model.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MilestoneRepository extends JpaRepository<Milestone, Long> {
}
