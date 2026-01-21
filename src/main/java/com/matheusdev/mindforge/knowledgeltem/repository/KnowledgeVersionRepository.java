package com.matheusdev.mindforge.knowledgeltem.repository;

import com.matheusdev.mindforge.knowledgeltem.model.KnowledgeVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeVersionRepository extends JpaRepository<KnowledgeVersion, Long> {

    List<KnowledgeVersion> findByKnowledgeItemIdOrderByCreatedAtDesc(Long knowledgeItemId);

    Optional<KnowledgeVersion> findTopByKnowledgeItemIdOrderByCreatedAtDesc(Long knowledgeItemId);

    long countByKnowledgeItemId(Long knowledgeItemId);

    void deleteByKnowledgeItemId(Long knowledgeItemId);
}
