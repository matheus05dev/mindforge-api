package com.matheusdev.mindforge.knowledgeltem.service;

import com.matheusdev.mindforge.exception.ResourceNotFoundException;
import com.matheusdev.mindforge.knowledgeltem.model.KnowledgeItem;
import com.matheusdev.mindforge.knowledgeltem.repository.KnowledgeItemRepository;
import com.matheusdev.mindforge.workspace.model.Workspace;
import com.matheusdev.mindforge.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeItemRepository repository;
    private final WorkspaceRepository workspaceRepository;

    public List<KnowledgeItem> getAllItems() {
        return repository.findAll();
    }

    public KnowledgeItem getItemById(Long id) {
        return repository.findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Item de conhecimento não encontrado com o id: " + id));
    }

    public KnowledgeItem createItem(KnowledgeItem item, String workspaceId) {
        Workspace workspace = resolveWorkspace(workspaceId);
        item.setWorkspace(workspace);
        return repository.save(item);
    }

    private Workspace resolveWorkspace(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            // Try default "Geral"
            return workspaceRepository.findByNameIgnoreCase("Geral")
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Workspace ID not provided and default 'Geral' not found."));
        }

        // Try lookup by name (case insensitive)
        return workspaceRepository.findByNameIgnoreCase(identifier)
                .orElseGet(() -> {
                    // If not found by name, try as ID if numeric
                    try {
                        Long id = Long.parseLong(identifier);
                        return workspaceRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with ID: " + id));
                    } catch (NumberFormatException e) {
                        throw new ResourceNotFoundException("Workspace not found with identifier: " + identifier);
                    }
                });
    }

    public KnowledgeItem updateItem(Long id, KnowledgeItem item) {
        KnowledgeItem existingItem = getItemById(id);
        existingItem.setTitle(item.getTitle());
        existingItem.setContent(item.getContent());
        existingItem.setTags(item.getTags());
        return repository.save(existingItem);
    }

    public void deleteItem(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Item de conhecimento não encontrado com o id: " + id);
        }
        repository.deleteById(id);
    }

    public List<KnowledgeItem> searchByTag(String tag) {
        return repository.findByTag(tag);
    }
}
