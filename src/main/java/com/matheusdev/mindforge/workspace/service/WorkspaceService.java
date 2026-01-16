package com.matheusdev.mindforge.workspace.service;

import com.matheusdev.mindforge.exception.ResourceNotFoundException;
import com.matheusdev.mindforge.workspace.model.Workspace;
import com.matheusdev.mindforge.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;

    public List<Workspace> findAll() {
        // No futuro, filtrar por userId
        return workspaceRepository.findAll();
    }

    public Workspace findById(Long id) {
        return workspaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace não encontrado com o id: " + id));
    }

    public Workspace create(Workspace workspace) {
        // No futuro, associar ao ownerId
        return workspaceRepository.save(workspace);
    }

    @Transactional
    public Workspace update(Long id, Workspace workspace) {
        Workspace existing = findById(id);
        existing.setName(workspace.getName());
        existing.setDescription(workspace.getDescription());
        existing.setType(workspace.getType());
        return workspaceRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Workspace workspace = findById(id);
        workspaceRepository.delete(workspace);
    }
}
