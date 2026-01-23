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

    @Operation(summary = "Get the general mind map")
    @GetMapping
    public ResponseEntity<MindMap> getMindMap() {
        return ResponseEntity.ok(service.getMindMap());
    }

    @Operation(summary = "Save the general mind map")
    @PostMapping
    public ResponseEntity<MindMap> saveMindMap(@RequestBody SaveMindMapRequest request) {
        return ResponseEntity.ok(service.saveMindMap(request.getNodesJson(), request.getEdgesJson()));
    }
}
