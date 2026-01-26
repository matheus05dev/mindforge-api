package com.matheusdev.mindforge.mindmap.repository;

import com.matheusdev.mindforge.mindmap.model.MindMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MindMapRepository extends JpaRepository<MindMap, Long> {
        List<MindMap> findByWorkspaceId(Long workspaceId);

        java.util.Optional<MindMap> findByName(String name);

        // Tenant-aware queries
        @org.springframework.data.jpa.repository.Query("SELECT m FROM MindMap m WHERE m.tenant.id = :tenantId")
        Page<MindMap> findByTenantId(@org.springframework.data.repository.query.Param("tenantId") Long tenantId,
                        Pageable pageable);

        @org.springframework.data.jpa.repository.Query("SELECT m FROM MindMap m WHERE m.tenant.id = :tenantId")
        List<MindMap> findByTenantId(@org.springframework.data.repository.query.Param("tenantId") Long tenantId);

        @org.springframework.data.jpa.repository.Query("SELECT m FROM MindMap m WHERE m.id = :id AND m.tenant.id = :tenantId")
        Optional<MindMap> findByIdAndTenantId(@org.springframework.data.repository.query.Param("id") Long id,
                        @org.springframework.data.repository.query.Param("tenantId") Long tenantId);

        @org.springframework.data.jpa.repository.Query("SELECT m FROM MindMap m WHERE m.workspace.id = :workspaceId AND m.tenant.id = :tenantId")
        List<MindMap> findByWorkspaceIdAndTenantId(
                        @org.springframework.data.repository.query.Param("workspaceId") Long workspaceId,
                        @org.springframework.data.repository.query.Param("tenantId") Long tenantId);

        @org.springframework.data.jpa.repository.Query("SELECT m FROM MindMap m WHERE m.name = :name AND m.tenant.id = :tenantId")
        Optional<MindMap> findByNameAndTenantId(@org.springframework.data.repository.query.Param("name") String name,
                        @org.springframework.data.repository.query.Param("tenantId") Long tenantId);
}
