package com.matheusdev.mindforge.knowledgeltem.service;

import com.matheusdev.mindforge.exception.ResourceNotFoundException;
import com.matheusdev.mindforge.knowledgeltem.model.KnowledgeItem;
import com.matheusdev.mindforge.knowledgeltem.repository.KnowledgeItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeItemRepository repository;

    public List<KnowledgeItem> getAllItems() {
        return repository.findAll();
    }

    public KnowledgeItem getItemById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item de conhecimento não encontrado com o id: " + id));
    }

    public KnowledgeItem createItem(KnowledgeItem item) {
        return repository.save(item);
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
