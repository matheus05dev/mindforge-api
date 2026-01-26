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
        Long tenantId = com.matheusdev.mindforge.core.tenant.context.TenantContext.getTenantId();
        // For simplicity, we'll use a single default Mind Map for now.
        // In the future, this can be expanded to support multiple maps per
        // user/workspace.
        return repository.findByNameAndTenantId("Geral", tenantId)
                .orElseGet(() -> {
                    MindMap newMap = MindMap.builder()
                            .name("Geral")
                            .nodesJson("[]")
                            .edgesJson("[]")
                            .build();
                    // Tenant listener will set tenant on save, but here we are building.
                    // On save calls below it will be handled.
                    return newMap;
                });
    }

    @Transactional
    public MindMap saveMindMap(String nodesJson, String edgesJson) {
        Long tenantId = com.matheusdev.mindforge.core.tenant.context.TenantContext.getTenantId();
        MindMap mindMap = repository.findByNameAndTenantId("Geral", tenantId)
                .orElse(MindMap.builder().name("Geral").build());

        mindMap.setNodesJson(nodesJson);
        mindMap.setEdgesJson(edgesJson);

        return repository.save(mindMap);
    }
}
