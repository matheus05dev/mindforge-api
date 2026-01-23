package com.matheusdev.mindforge.study.roadmap.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "roadmap_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "roadmap_id")
    @JsonBackReference
    private Roadmap roadmap;

    private int orderIndex; // 1, 2, 3...

    private String title; // "Week 1: Basics"

    @Column(columnDefinition = "TEXT")
    private String description; // "Learn variables, loops..."

    @Column(columnDefinition = "TEXT")
    private String resourcesJson; // JSON string containing list of resources (Title, URL, Type)
}
