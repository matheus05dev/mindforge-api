package com.matheusdev.mindforge.project.decision.controller;

import com.matheusdev.mindforge.project.decision.dto.DecisionRequest;
import com.matheusdev.mindforge.project.decision.dto.DecisionResponse;
import com.matheusdev.mindforge.project.decision.service.DecisionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/decisions")
@RequiredArgsConstructor
public class DecisionController {

    private final DecisionService decisionService;

    @GetMapping
    public ResponseEntity<List<DecisionResponse>> getDecisions(@PathVariable Long projectId) {
        return ResponseEntity.ok(decisionService.getDecisionsByProject(projectId));
    }

    @PostMapping
    public ResponseEntity<DecisionResponse> createDecision(@PathVariable Long projectId,
            @Valid @RequestBody DecisionRequest request) {
        request.setProjectId(projectId);
        return ResponseEntity.ok(decisionService.createDecision(request));
    }

    @PutMapping("/{decisionId}")
    public ResponseEntity<DecisionResponse> updateDecision(
            @PathVariable Long projectId,
            @PathVariable Long decisionId,
            @Valid @RequestBody DecisionRequest request) {
        // Ignoramos o projectId do path no update, confiando no ID da decisão,
        // mas seria boa prática validar se a decisão pertence ao projeto.
        return ResponseEntity.ok(decisionService.updateDecision(decisionId, request));
    }

    @PostMapping("/propose")
    public ResponseEntity<DecisionResponse> proposeDecision(
            @PathVariable Long projectId,
            @RequestBody Map<String, String> payload) {
        String context = payload.get("context");
        return ResponseEntity.ok(decisionService.proposeDecisionFromContext(projectId, context));
    }

    @GetMapping("/metrics")
    public ResponseEntity<com.matheusdev.mindforge.project.decision.dto.DecisionMetricsDTO> getMetrics(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(decisionService.getProjectMetrics(projectId));
    }
}
