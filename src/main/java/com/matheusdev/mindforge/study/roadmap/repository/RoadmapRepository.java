package com.matheusdev.mindforge.study.roadmap.repository;

import com.matheusdev.mindforge.study.roadmap.model.Roadmap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoadmapRepository extends JpaRepository<Roadmap, Long> {
    // List<Roadmap> findByUserIdOrderByCreatedAtDesc(Long userId); // Deprecated in
    // favor of tenant isolation

    List<Roadmap> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    java.util.Optional<Roadmap> findByIdAndTenantId(Long id, Long tenantId);
}
