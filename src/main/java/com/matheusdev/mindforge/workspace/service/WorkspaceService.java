package com.matheusdev.mindforge.workspace.service;

import com.matheusdev.mindforge.workspace.model.Workspace;
import com.matheusdev.mindforge.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
                .orElseThrow(() -> new RuntimeException("Workspace not found"));
    }

    public Workspace create(Workspace workspace) {
        // No futuro, associar ao ownerId
        return workspaceRepository.save(workspace);
    }
}
