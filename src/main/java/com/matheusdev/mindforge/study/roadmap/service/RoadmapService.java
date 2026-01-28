package com.matheusdev.mindforge.study.roadmap.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusdev.mindforge.ai.service.AIOrchestrationService;
import com.matheusdev.mindforge.study.roadmap.dto.RoadmapDTOs;
import com.matheusdev.mindforge.study.roadmap.model.Roadmap;
import com.matheusdev.mindforge.study.roadmap.model.RoadmapItem;
import com.matheusdev.mindforge.study.roadmap.repository.RoadmapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoadmapService {

    private final RoadmapRepository roadmapRepository;
    private final AIOrchestrationService aiOrchestrationService;
    private final ObjectMapper objectMapper;

    @Transactional
    public RoadmapDTOs.RoadmapResponse generateAndSaveRoadmap(String topic, String duration,
            String difficulty) {
        Long userId = com.matheusdev.mindforge.core.auth.util.SecurityUtils.getCurrentUserId();

        // 1. Generate via AI (Sync for now, ideally async job)
        RoadmapDTOs.RoadmapResponse generatedData = aiOrchestrationService
                .generateRoadmap(topic, duration, difficulty)
                .join();

        // 2. Map to Entity
        Roadmap roadmap = Roadmap.builder()
                .title(generatedData.getTitle())
                .description(generatedData.getDescription())
                .targetAudience(generatedData.getTargetAudience())
                .userId(userId)
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();

        // 3. Map Items
        for (RoadmapDTOs.RoadmapItemResponse itemDto : generatedData.getItems()) {
            try {
                String resourcesJson = objectMapper.writeValueAsString(itemDto.getResources());

                RoadmapItem item = RoadmapItem.builder()
                        .roadmap(roadmap)
                        .title(itemDto.getTitle())
                        .description(itemDto.getDescription())
                        .orderIndex(itemDto.getOrderIndex())
                        .resourcesJson(resourcesJson)
                        .build();

                roadmap.getItems().add(item);
            } catch (JsonProcessingException e) {
                log.error("Error serializing resources for item {}", itemDto.getTitle(), e);
            }
        }

        // 4. Save
        Roadmap savedRoadmap = roadmapRepository.save(roadmap);

        // 5. Return DTO with ID
        return toResponse(savedRoadmap);
    }

    public List<RoadmapDTOs.RoadmapResponse> getUserRoadmaps() {
        Long tenantId = com.matheusdev.mindforge.core.auth.util.SecurityUtils.getCurrentTenantId();
        return roadmapRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public RoadmapDTOs.RoadmapResponse getRoadmap(Long id) {
        Long tenantId = com.matheusdev.mindforge.core.auth.util.SecurityUtils.getCurrentTenantId();
        Roadmap roadmap = roadmapRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Roadmap not found"));
        return toResponse(roadmap);
    }

    private RoadmapDTOs.RoadmapResponse toResponse(Roadmap entity) {
        List<RoadmapDTOs.RoadmapItemResponse> items = entity.getItems().stream()
                .map(item -> {
                    List<RoadmapDTOs.ResourceLink> resources = new ArrayList<>();
                    try {
                        if (item.getResourcesJson() != null) {
                            resources = objectMapper.readValue(
                                    item.getResourcesJson(),
                                    new com.fasterxml.jackson.core.type.TypeReference<List<RoadmapDTOs.ResourceLink>>() {
                                    });
                        }
                    } catch (JsonProcessingException e) {
                        log.error("Error parsing resources for item {}", item.getId());
                    }

                    return RoadmapDTOs.RoadmapItemResponse.builder()
                            .id(item.getId())
                            .orderIndex(item.getOrderIndex())
                            .title(item.getTitle())
                            .description(item.getDescription())
                            .resources(resources)
                            .build();
                })
                .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                .collect(Collectors.toList());

        return RoadmapDTOs.RoadmapResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .targetAudience(entity.getTargetAudience())
                .createdAt(entity.getCreatedAt().toString())
                .items(items)
                .build();
    }
}
