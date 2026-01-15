package com.matheusdev.mindforge.kanban.repository;

import com.matheusdev.mindforge.kanban.model.KanbanTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KanbanTaskRepository extends JpaRepository<KanbanTask, Long> {

    // Encontra tarefas cujo título contém a string fornecida, ignorando maiúsculas/minúsculas.
    // Ótimo para testar funcionalidades de busca.
    List<KanbanTask> findByTitleContainingIgnoreCase(String title);

    // Encontra todas as tarefas associadas a um ID de projeto específico.
    // Essencial para testar a integridade dos relacionamentos.
    List<KanbanTask> findByProjectId(Long projectId);
}
