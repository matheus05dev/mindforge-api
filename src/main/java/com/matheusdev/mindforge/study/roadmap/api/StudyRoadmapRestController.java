package com.matheusdev.mindforge.study.roadmap.api;

import com.matheusdev.mindforge.study.roadmap.dto.RoadmapDTOs;
import com.matheusdev.mindforge.study.roadmap.service.RoadmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/studies/roadmaps")
@RequiredArgsConstructor
public class StudyRoadmapRestController {

    private final RoadmapService roadmapService;

    @PostMapping("/generate")
    public ResponseEntity<RoadmapDTOs.RoadmapResponse> generateRoadmap(
            @RequestBody RoadmapDTOs.CreateRoadmapRequest request) {
        // userId obtained from context in service
        return ResponseEntity.ok(roadmapService.generateAndSaveRoadmap(
                request.getTopic(),
                request.getDuration(),
                request.getDifficulty()));
    }

    @GetMapping
    public ResponseEntity<List<RoadmapDTOs.RoadmapResponse>> getUserRoadmaps() {
        return ResponseEntity.ok(roadmapService.getUserRoadmaps());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoadmapDTOs.RoadmapResponse> getRoadmap(@PathVariable Long id) {
        return ResponseEntity.ok(roadmapService.getRoadmap(id));
    }
}
