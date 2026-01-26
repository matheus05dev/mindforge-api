package com.matheusdev.mindforge.project.decision.repository;

import com.matheusdev.mindforge.project.decision.model.DecisionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DecisionRepository extends JpaRepository<DecisionRecord, Long> {
    List<DecisionRecord> findByProjectId(Long projectId);

    List<DecisionRecord> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    // Tenant-aware queries
    @org.springframework.data.jpa.repository.Query("SELECT d FROM DecisionRecord d WHERE d.project.tenant.id = :tenantId")
    Page<DecisionRecord> findByTenantId(@org.springframework.data.repository.query.Param("tenantId") Long tenantId,
            Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT d FROM DecisionRecord d WHERE d.project.tenant.id = :tenantId")
    List<DecisionRecord> findByTenantId(@org.springframework.data.repository.query.Param("tenantId") Long tenantId);

    @org.springframework.data.jpa.repository.Query("SELECT d FROM DecisionRecord d WHERE d.id = :id AND d.project.tenant.id = :tenantId")
    Optional<DecisionRecord> findByIdAndTenantId(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("tenantId") Long tenantId);
}
