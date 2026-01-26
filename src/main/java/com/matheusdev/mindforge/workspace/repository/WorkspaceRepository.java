package com.matheusdev.mindforge.workspace.repository;

import com.matheusdev.mindforge.workspace.model.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    java.util.Optional<Workspace> findByNameIgnoreCase(String name);

    boolean existsByNameAndTenantId(String name, Long tenantId);

    // Tenant-aware queries
    java.util.List<Workspace> findByTenantId(Long tenantId);

    java.util.Optional<Workspace> findByIdAndTenantId(Long id, Long tenantId);

    @org.springframework.data.jpa.repository.Query("SELECT w FROM Workspace w WHERE LOWER(w.name) = LOWER(:name) AND w.tenant.id = :tenantId")
    java.util.Optional<Workspace> findByNameIgnoreCaseAndTenantId(
            @org.springframework.data.repository.query.Param("name") String name,
            @org.springframework.data.repository.query.Param("tenantId") Long tenantId);
}
