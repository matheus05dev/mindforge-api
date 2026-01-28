package com.matheusdev.mindforge.study.subject.repository;

import com.matheusdev.mindforge.core.tenant.context.TenantContext;
import com.matheusdev.mindforge.core.tenant.model.Tenant;
import com.matheusdev.mindforge.study.subject.model.Subject;
import com.matheusdev.mindforge.study.subject.model.enums.ProficiencyLevel;
import com.matheusdev.mindforge.workspace.model.Workspace;
import com.matheusdev.mindforge.workspace.model.WorkspaceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class SubjectRepositoryIT {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SubjectRepository subjectRepository;

    private Tenant tenant1;
    private Tenant tenant2;
    private Workspace workspace1;
    private Workspace workspace2;

    @BeforeEach
    void setUp() {
        tenant1 = Tenant.builder().name("Tenant 1").slug("tenant-1").build();
        tenant2 = Tenant.builder().name("Tenant 2").slug("tenant-2").build();
        entityManager.persist(tenant1);
        entityManager.persist(tenant2);

        workspace1 = new Workspace();
        workspace1.setName("Workspace 1");
        workspace1.setTenant(tenant1);
        workspace1.setType(WorkspaceType.STUDY);
        entityManager.persist(workspace1);

        workspace2 = new Workspace();
        workspace2.setName("Workspace 2");
        workspace2.setTenant(tenant2);
        workspace2.setType(WorkspaceType.STUDY);
        entityManager.persist(workspace2);

        entityManager.flush();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should find subjects by tenant ID and ensure isolation")
    void shouldFindSubjectsByTenantId() {
        // Arrange
        TenantContext.setTenantId(tenant1.getId());
        Subject subject = new Subject();
        subject.setName("Java");
        subject.setWorkspace(workspace1);
        subject.setProficiencyLevel(ProficiencyLevel.INTERMEDIATE);
        entityManager.persist(subject);

        TenantContext.setTenantId(tenant2.getId());
        Subject subject2 = new Subject();
        subject2.setName("Python");
        subject2.setWorkspace(workspace2);
        subject2.setProficiencyLevel(ProficiencyLevel.BEGINNER);
        entityManager.persist(subject2);

        entityManager.flush();

        // Act
        List<Subject> tenant1Subjects = subjectRepository.findByTenantId(tenant1.getId());
        List<Subject> tenant2Subjects = subjectRepository.findByTenantId(tenant2.getId());

        // Assert
        assertThat(tenant1Subjects).hasSize(1);
        assertThat(tenant1Subjects.get(0).getName()).isEqualTo("Java");
        assertThat(tenant1Subjects.get(0).getTenant().getId()).isEqualTo(tenant1.getId());

        assertThat(tenant2Subjects).hasSize(1);
        assertThat(tenant2Subjects.get(0).getName()).isEqualTo("Python");
        assertThat(tenant2Subjects.get(0).getTenant().getId()).isEqualTo(tenant2.getId());
    }

    @Test
    @DisplayName("Should find subject by ID and tenant ID")
    void shouldFindByIdAndTenantId() {
        // Arrange
        TenantContext.setTenantId(tenant1.getId());
        Subject subject = new Subject();
        subject.setName("Java");
        subject.setWorkspace(workspace1);
        entityManager.persist(subject);
        entityManager.flush();

        // Act
        Optional<Subject> found = subjectRepository.findByIdAndTenantId(subject.getId(), tenant1.getId());
        Optional<Subject> notFound = subjectRepository.findByIdAndTenantId(subject.getId(), tenant2.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Java");
        assertThat(notFound).isEmpty();
    }
}
