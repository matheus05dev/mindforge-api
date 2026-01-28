package com.matheusdev.mindforge.mindmap.api;

import com.matheusdev.mindforge.mindmap.model.MindMap;
import com.matheusdev.mindforge.mindmap.service.MindMapService;
import com.matheusdev.mindforge.mindmap.dto.SaveMindMapRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mind-map")
@RequiredArgsConstructor
@Tag(name = "Mind Map", description = "Mind Map management")
public class MindMapRestController {

    private final MindMapService service;

    private com.matheusdev.mindforge.mindmap.dto.MindMapResponse toResponse(MindMap mindMap) {
        return com.matheusdev.mindforge.mindmap.dto.MindMapResponse.builder()
                .id(mindMap.getId())
                .name(mindMap.getName())
                .nodesJson(mindMap.getNodesJson())
                .edgesJson(mindMap.getEdgesJson())
                .workspaceId(mindMap.getWorkspace() != null ? mindMap.getWorkspace().getId() : null)
                .build();
    }

    @Operation(summary = "Get the general mind map")
    @GetMapping
    public ResponseEntity<com.matheusdev.mindforge.mindmap.dto.MindMapResponse> getMindMap() {
        MindMap mindMap = service.getMindMap();
        return ResponseEntity.ok(toResponse(mindMap));
    }

    @Operation(summary = "Save the general mind map")
    @PostMapping
    public ResponseEntity<com.matheusdev.mindforge.mindmap.dto.MindMapResponse> saveMindMap(
            @RequestBody SaveMindMapRequest request) {
        MindMap mindMap = service.saveMindMap(request.getNodesJson(), request.getEdgesJson());
        return ResponseEntity.ok(toResponse(mindMap));
    }
}
