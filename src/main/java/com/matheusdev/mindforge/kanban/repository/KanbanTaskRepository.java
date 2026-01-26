package com.matheusdev.mindforge.kanban.repository;

import com.matheusdev.mindforge.kanban.model.KanbanTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KanbanTaskRepository extends JpaRepository<KanbanTask, Long> {
    List<KanbanTask> findByColumnId(Long columnId);

    List<KanbanTask> findByTitleContainingIgnoreCase(String title);

    List<KanbanTask> findByProjectId(Long projectId);

    // Tenant-aware queries
    // Assuming Task belongs to Project or Subject which has Tenant.
    // Need to handle both cases via LEFT JOINs or check Column -> Board/Project ->
    // Tenant relationship?
    // Based on Entity: has `project` and `subject` optional relationships.
    @org.springframework.data.jpa.repository.Query("SELECT t FROM KanbanTask t LEFT JOIN t.project p LEFT JOIN t.subject s WHERE (p.tenant.id = :tenantId OR s.tenant.id = :tenantId)")
    Page<KanbanTask> findByTenantId(@org.springframework.data.repository.query.Param("tenantId") Long tenantId,
            Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM KanbanTask t LEFT JOIN t.project p LEFT JOIN t.subject s WHERE (p.tenant.id = :tenantId OR s.tenant.id = :tenantId)")
    List<KanbanTask> findByTenantId(@org.springframework.data.repository.query.Param("tenantId") Long tenantId);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM KanbanTask t LEFT JOIN t.project p LEFT JOIN t.subject s WHERE t.id = :id AND (p.tenant.id = :tenantId OR s.tenant.id = :tenantId)")
    Optional<KanbanTask> findByIdAndTenantId(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("tenantId") Long tenantId);
}
