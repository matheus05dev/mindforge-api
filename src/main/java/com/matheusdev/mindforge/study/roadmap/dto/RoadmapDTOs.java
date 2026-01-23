package com.matheusdev.mindforge.study.roadmap.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class RoadmapDTOs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRoadmapRequest {
        private String topic;
        private String duration; // e.g., "4 weeks"
        private String difficulty; // "Beginner", "Advanced"
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoadmapResponse {
        private Long id;
        private String title;
        private String description;
        private String targetAudience;
        private List<RoadmapItemResponse> items;
        private String createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoadmapItemResponse {
        private Long id;
        private int orderIndex;
        private String title;
        private String description;
        private List<ResourceLink> resources;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResourceLink {
        private String title;
        private String url;
        private String type; // "Video", "Article", "Course"
    }
}
