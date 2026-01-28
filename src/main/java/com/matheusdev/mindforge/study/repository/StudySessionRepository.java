package com.matheusdev.mindforge.study.repository;

import com.matheusdev.mindforge.study.model.StudySession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudySessionRepository extends JpaRepository<StudySession, Long> {
        List<StudySession> findBySubjectId(Long subjectId);

        // Tenant-aware queries
        @org.springframework.data.jpa.repository.Query("SELECT s FROM StudySession s WHERE s.subject.tenant.id = :tenantId")
        Page<StudySession> findByTenantId(@org.springframework.data.repository.query.Param("tenantId") Long tenantId,
                        Pageable pageable);

        @org.springframework.data.jpa.repository.Query("SELECT s FROM StudySession s WHERE s.subject.tenant.id = :tenantId")
        List<StudySession> findByTenantId(@org.springframework.data.repository.query.Param("tenantId") Long tenantId);

        @org.springframework.data.jpa.repository.Query("SELECT s FROM StudySession s WHERE s.id = :id AND s.subject.tenant.id = :tenantId")
        Optional<StudySession> findByIdAndTenantId(@org.springframework.data.repository.query.Param("id") Long id,
                        @org.springframework.data.repository.query.Param("tenantId") Long tenantId);
}
