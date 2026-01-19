package com.matheusdev.mindforge.ai.service.model;

public record Evidence(
        String documentId,
        String section,
        Integer page,
        String contentType, // text | table | formula
        String excerpt,
        double score,
        java.util.Map<String, Object> metadata) {
}
