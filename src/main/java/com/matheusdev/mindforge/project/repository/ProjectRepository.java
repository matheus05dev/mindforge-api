package com.matheusdev.mindforge.project.repository;

import com.matheusdev.mindforge.project.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByNameContainingIgnoreCase(String name);

    List<Project> findByWorkspaceId(Long workspaceId);

    Optional<Project> findByIdAndWorkspaceId(Long projectId, Long workspaceId);

    boolean existsByIdAndWorkspaceId(Long projectId, Long workspaceId);
}
