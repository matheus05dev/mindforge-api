package com.matheusdev.mindforge.study.resource.api;

import com.matheusdev.mindforge.study.resource.dto.StudyResourceRequest;
import com.matheusdev.mindforge.study.resource.dto.StudyResourceResponse;
import com.matheusdev.mindforge.study.resource.service.StudyResourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/studies")
@RequiredArgsConstructor
public class StudyResourceRestController {

    private final StudyResourceService resourceService;

    @GetMapping("/subjects/{subjectId}/resources")
    public ResponseEntity<List<StudyResourceResponse>> getResourcesBySubject(@PathVariable Long subjectId) {
        return ResponseEntity.ok(resourceService.getResourcesBySubject(subjectId));
    }

    @PostMapping("/subjects/{subjectId}/resources")
    public ResponseEntity<StudyResourceResponse> createResource(
            @PathVariable Long subjectId,
            @Valid @RequestBody StudyResourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resourceService.createResource(subjectId, request));
    }

    @DeleteMapping("/resources/{resourceId}")
    public ResponseEntity<Void> deleteResource(@PathVariable Long resourceId) {
        resourceService.deleteResource(resourceId);
        return ResponseEntity.noContent().build();
    }
}
