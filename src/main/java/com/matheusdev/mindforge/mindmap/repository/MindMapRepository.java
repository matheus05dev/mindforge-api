package com.matheusdev.mindforge.mindmap.repository;

import com.matheusdev.mindforge.mindmap.model.MindMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MindMapRepository extends JpaRepository<MindMap, Long> {
    Optional<MindMap> findByName(String name);
}
