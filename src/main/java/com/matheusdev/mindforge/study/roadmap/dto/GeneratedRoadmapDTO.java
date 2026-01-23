package com.matheusdev.mindforge.study.roadmap.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class GeneratedRoadmapDTO {
    private String title;
    private String description;
    private List<GeneratedItemDTO> items;

    @Data
    @NoArgsConstructor
    public static class GeneratedItemDTO {
        private String title;
        private String description;
        private String searchQuery; // Helper for retrieving resources
    }
}
