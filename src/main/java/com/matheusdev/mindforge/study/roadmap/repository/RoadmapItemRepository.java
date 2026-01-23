package com.matheusdev.mindforge.study.roadmap.repository;

import com.matheusdev.mindforge.study.roadmap.model.RoadmapItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoadmapItemRepository extends JpaRepository<RoadmapItem, Long> {
}
