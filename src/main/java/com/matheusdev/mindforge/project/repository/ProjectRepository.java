package com.matheusdev.mindforge.project.repository;

import com.matheusdev.mindforge.project.model.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByNameContainingIgnoreCase(String name);

    List<Project> findByWorkspaceId(Long workspaceId);

    // Tenant-aware queries
    Page<Project> findByTenantId(Long tenantId, Pageable pageable);

    List<Project> findByTenantId(Long tenantId);

    Optional<Project> findByIdAndTenantId(Long id, Long tenantId);

    Page<Project> findByWorkspaceIdAndTenantId(Long workspaceId, Long tenantId, Pageable pageable);

    // Optimized: Using EntityGraph to avoid cartesian product from multiple LEFT
    // JOIN FETCH
    // Fetch milestones and documents in separate queries to prevent multiplication
    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.milestones WHERE p.workspace.id = :workspaceId")
    List<Project> findAllByWorkspaceIdWithMilestones(@Param("workspaceId") Long workspaceId);

    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.documents WHERE p.workspace.id = :workspaceId")
    List<Project> findAllByWorkspaceIdWithDocuments(@Param("workspaceId") Long workspaceId);

    Page<Project> findByWorkspaceId(Long workspaceId, Pageable pageable);

    Optional<Project> findByIdAndWorkspaceId(Long projectId, Long workspaceId);

    boolean existsByIdAndWorkspaceId(Long projectId, Long workspaceId);
}
