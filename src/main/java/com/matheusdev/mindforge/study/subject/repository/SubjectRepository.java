package com.matheusdev.mindforge.study.subject.repository;

import com.matheusdev.mindforge.study.subject.model.Subject;
import com.matheusdev.mindforge.study.subject.model.enums.ProficiencyLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByProficiencyLevel(ProficiencyLevel level);

    List<Subject> findByWorkspaceId(Long workspaceId);

    Page<Subject> findByWorkspaceId(Long workspaceId, Pageable pageable);

    // Tenant-aware queries
    Page<Subject> findByTenantId(Long tenantId, Pageable pageable);

    List<Subject> findByTenantId(Long tenantId);

    java.util.Optional<Subject> findByIdAndTenantId(Long id, Long tenantId);

    Page<Subject> findByWorkspaceIdAndTenantId(Long workspaceId, Long tenantId, Pageable pageable);
}