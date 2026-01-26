package com.matheusdev.mindforge.study.note.repository;

import com.matheusdev.mindforge.study.note.model.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyNoteRepository extends JpaRepository<Note, Long> {
    List<Note> findBySubjectId(Long subjectId);

    List<Note> findBySubjectIdOrderByUpdatedAtDesc(Long subjectId);

    // Tenant-aware queries
    @org.springframework.data.jpa.repository.Query("SELECT n FROM StudyNote n WHERE n.subject.tenant.id = :tenantId")
    Page<Note> findByTenantId(@org.springframework.data.repository.query.Param("tenantId") Long tenantId,
            Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT n FROM StudyNote n WHERE n.subject.tenant.id = :tenantId")
    List<Note> findByTenantId(@org.springframework.data.repository.query.Param("tenantId") Long tenantId);

    @org.springframework.data.jpa.repository.Query("SELECT n FROM StudyNote n WHERE n.id = :id AND n.subject.tenant.id = :tenantId")
    Optional<Note> findByIdAndTenantId(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("tenantId") Long tenantId);
}
