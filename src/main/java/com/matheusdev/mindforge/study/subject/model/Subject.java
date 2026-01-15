package com.matheusdev.mindforge.study.subject.model;

import com.matheusdev.mindforge.study.model.StudySession;
import com.matheusdev.mindforge.study.subject.model.enums.ProficiencyLevel;
import com.matheusdev.mindforge.study.subject.model.enums.ProfessionalLevel;
import com.matheusdev.mindforge.workspace.model.Workspace;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "subjects")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    private ProficiencyLevel proficiencyLevel;

    @Enumerated(EnumType.STRING)
    private ProfessionalLevel professionalLevel;

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudySession> studySessions = new ArrayList<>();

    @Repository
    public interface SubjectRepository extends JpaRepository<Subject, Long> {
        List<Subject> findByProficiencyLevel(ProficiencyLevel level);
        List<Subject> findByWorkspaceId(Long workspaceId);
    }
}
