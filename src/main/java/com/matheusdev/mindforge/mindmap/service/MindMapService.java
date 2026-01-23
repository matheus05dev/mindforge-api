package com.matheusdev.mindforge.mindmap.service;

import com.matheusdev.mindforge.mindmap.model.MindMap;
import com.matheusdev.mindforge.mindmap.repository.MindMapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MindMapService {

    private final MindMapRepository repository;

    @Transactional(readOnly = true)
    public MindMap getMindMap() {
        // For simplicity, we'll use a single default Mind Map for now.
        // In the future, this can be expanded to support multiple maps per
        // user/workspace.
        return repository.findByName("Geral")
                .orElseGet(() -> MindMap.builder()
                        .name("Geral")
                        .nodesJson("[]")
                        .edgesJson("[]")
                        .build());
    }

    @Transactional
    public MindMap saveMindMap(String nodesJson, String edgesJson) {
        MindMap mindMap = repository.findByName("Geral")
                .orElse(MindMap.builder().name("Geral").build());

        mindMap.setNodesJson(nodesJson);
        mindMap.setEdgesJson(edgesJson);

        return repository.save(mindMap);
    }
}
