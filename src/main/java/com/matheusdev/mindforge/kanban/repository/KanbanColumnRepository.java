package com.matheusdev.mindforge.kanban.repository;

import com.matheusdev.mindforge.kanban.model.KanbanColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KanbanColumnRepository extends JpaRepository<KanbanColumn, Long> {
}
