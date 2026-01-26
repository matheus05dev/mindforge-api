package com.matheusdev.mindforge.study.quiz.repository;

import com.matheusdev.mindforge.study.quiz.model.Quiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findBySubjectId(Long subjectId);

    // Tenant-aware queries
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Quiz q WHERE q.subject.tenant.id = :tenantId")
    Page<Quiz> findByTenantId(@org.springframework.data.repository.query.Param("tenantId") Long tenantId,
            Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT q FROM Quiz q WHERE q.subject.tenant.id = :tenantId")
    List<Quiz> findByTenantId(@org.springframework.data.repository.query.Param("tenantId") Long tenantId);

    @org.springframework.data.jpa.repository.Query("SELECT q FROM Quiz q WHERE q.id = :id AND q.subject.tenant.id = :tenantId")
    Optional<Quiz> findByIdAndTenantId(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("tenantId") Long tenantId);
}
