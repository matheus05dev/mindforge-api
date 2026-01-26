package com.matheusdev.mindforge.document.repository;

import com.matheusdev.mindforge.document.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByProjectId(Long projectId);

    // Tenant-aware queries (Document doesn't have tenant_id directly, filtered via
    // relationships)
    List<Document> findByWorkspaceId(Long workspaceId);
}
