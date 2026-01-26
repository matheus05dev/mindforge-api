package com.matheusdev.mindforge.kanban.repository;

import com.matheusdev.mindforge.kanban.model.KanbanColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KanbanColumnRepository extends JpaRepository<KanbanColumn, Long> {

    @org.springframework.data.jpa.repository.Query("SELECT c FROM KanbanColumn c WHERE c.board.tenant.id = :tenantId")
    java.util.List<KanbanColumn> findAllByTenantId(Long tenantId);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM KanbanColumn c WHERE c.id = :id AND c.board.tenant.id = :tenantId")
    java.util.Optional<KanbanColumn> findByIdAndTenantId(Long id, Long tenantId);
}
