package com.matheusdev.mindforge.study.service;

import com.matheusdev.mindforge.core.tenant.context.TenantContext;
import com.matheusdev.mindforge.exception.ResourceNotFoundException;
import com.matheusdev.mindforge.study.dto.StudySessionRequest;
import com.matheusdev.mindforge.study.dto.StudySessionResponse;
import com.matheusdev.mindforge.study.mapper.StudyMapper;
import com.matheusdev.mindforge.study.model.StudySession;
import com.matheusdev.mindforge.study.repository.StudySessionRepository;
import com.matheusdev.mindforge.study.subject.dto.SubjectRequest;
import com.matheusdev.mindforge.study.subject.dto.SubjectResponse;
import com.matheusdev.mindforge.study.subject.model.Subject;
import com.matheusdev.mindforge.study.subject.repository.SubjectRepository;
import com.matheusdev.mindforge.workspace.model.Workspace;
import com.matheusdev.mindforge.workspace.model.WorkspaceType;
import com.matheusdev.mindforge.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudyServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private StudySessionRepository studySessionRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private StudyMapper mapper;

    @InjectMocks
    private StudyService studyService;

    private MockedStatic<TenantContext> tenantContextMock;
    private Subject testSubject;
    private StudySession testSession;
    private Workspace testWorkspace;
    private static final Long TENANT_ID = 1L;
    private static final Long SUBJECT_ID = 1L;
    private static final Long WORKSPACE_ID = 1L;
    private static final Long SESSION_ID = 1L;

    @BeforeEach
    void setUp() {
        tenantContextMock = mockStatic(TenantContext.class);

        testWorkspace = new Workspace();
        testWorkspace.setId(WORKSPACE_ID);
        testWorkspace.setName("Test Workspace");
        testWorkspace.setType(WorkspaceType.STUDY);
        testWorkspace.setTenantId(TENANT_ID);

        testSubject = new Subject();
        testSubject.setId(SUBJECT_ID);
        testSubject.setName("Mathematics");
        testSubject.setDescription("Advanced Mathematics");
        testSubject.setWorkspace(testWorkspace);
        testSubject.setTenantId(TENANT_ID);

        testSession = new StudySession();
        testSession.setId(SESSION_ID);
        testSession.setSubject(testSubject);
        testSession.setDurationMinutes(60);
        testSession.setNotes("Study notes");
        testSession.setStartTime(LocalDateTime.now());
    }

    @AfterEach
    void tearDown() {
        tenantContextMock.close();
    }

    @Test
    @DisplayName("Should return all subjects when tenant context is set")
    void getAllSubjects_ShouldReturnSubjects_WhenTenantContextIsSet() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Subject> subjectPage = new PageImpl<>(Arrays.asList(testSubject));
        SubjectResponse response = new SubjectResponse();
        response.setId(SUBJECT_ID);
        response.setName("Mathematics");

        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(subjectRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(subjectPage);
        when(mapper.toResponse(testSubject)).thenReturn(response);

        // Act
        Page<SubjectResponse> result = studyService.getAllSubjects(pageable, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Mathematics", result.getContent().get(0).getName());
        verify(subjectRepository).findByTenantId(TENANT_ID, pageable);
    }

    @Test
    @DisplayName("Should throw exception when tenant context is not set")
    void getAllSubjects_ShouldThrowException_WhenTenantContextIsNull() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        tenantContextMock.when(TenantContext::getTenantId).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> studyService.getAllSubjects(pageable, null));
        assertEquals("Tenant context not set", exception.getMessage());
    }

    @Test
    @DisplayName("Should return subject by ID when it exists")
    void getSubjectById_ShouldReturnSubject_WhenSubjectExists() {
        // Arrange
        SubjectResponse response = new SubjectResponse();
        response.setId(SUBJECT_ID);
        response.setName("Mathematics");

        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(subjectRepository.findByIdAndTenantId(SUBJECT_ID, TENANT_ID))
                .thenReturn(Optional.of(testSubject));
        when(mapper.toResponse(testSubject)).thenReturn(response);

        // Act
        SubjectResponse result = studyService.getSubjectById(SUBJECT_ID);

        // Assert
        assertNotNull(result);
        assertEquals(SUBJECT_ID, result.getId());
        assertEquals("Mathematics", result.getName());
        verify(subjectRepository).findByIdAndTenantId(SUBJECT_ID, TENANT_ID);
    }

    @Test
    @DisplayName("Should throw exception when subject not found")
    void getSubjectById_ShouldThrowException_WhenSubjectNotFound() {
        // Arrange
        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(subjectRepository.findByIdAndTenantId(SUBJECT_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> studyService.getSubjectById(SUBJECT_ID));
        assertTrue(exception.getMessage().contains("Assunto de estudo não encontrado"));
    }

    @Test
    @DisplayName("Should create subject with workspace")
    void createSubject_ShouldCreateSubject_WhenWorkspaceExists() {
        // Arrange
        SubjectRequest request = new SubjectRequest();
        request.setName("Physics");
        request.setWorkspaceId(WORKSPACE_ID);

        Subject newSubject = new Subject();
        newSubject.setName("Physics");

        SubjectResponse response = new SubjectResponse();
        response.setId(2L);
        response.setName("Physics");

        when(mapper.toEntity(request)).thenReturn(newSubject);
        when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.of(testWorkspace));
        when(subjectRepository.save(newSubject)).thenReturn(newSubject);
        when(mapper.toResponse(newSubject)).thenReturn(response);

        // Act
        SubjectResponse result = studyService.createSubject(request);

        // Assert
        assertNotNull(result);
        assertEquals("Physics", result.getName());
        verify(workspaceRepository).findById(WORKSPACE_ID);
        verify(subjectRepository).save(newSubject);
    }

    @Test
    @DisplayName("Should throw exception when creating subject without workspace")
    void createSubject_ShouldThrowException_WhenWorkspaceIdIsNull() {
        // Arrange
        SubjectRequest request = new SubjectRequest();
        request.setName("Physics");
        request.setWorkspaceId(null);

        Subject newSubject = new Subject();
        when(mapper.toEntity(request)).thenReturn(newSubject);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> studyService.createSubject(request));
        assertTrue(exception.getMessage().contains("Workspace ID é obrigatório"));
    }

    @Test
    @DisplayName("Should update subject successfully")
    void updateSubject_ShouldUpdateSubject() {
        // Arrange
        SubjectRequest updateRequest = new SubjectRequest();
        updateRequest.setName("Updated Mathematics");

        SubjectResponse response = new SubjectResponse();
        response.setId(SUBJECT_ID);
        response.setName("Updated Mathematics");

        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(subjectRepository.findByIdAndTenantId(SUBJECT_ID, TENANT_ID))
                .thenReturn(Optional.of(testSubject));
        doNothing().when(mapper).updateSubjectFromRequest(updateRequest, testSubject);
        when(subjectRepository.save(testSubject)).thenReturn(testSubject);
        when(mapper.toResponse(testSubject)).thenReturn(response);

        // Act
        SubjectResponse result = studyService.updateSubject(SUBJECT_ID, updateRequest);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Mathematics", result.getName());
        verify(mapper).updateSubjectFromRequest(updateRequest, testSubject);
        verify(subjectRepository).save(testSubject);
    }

    @Test
    @DisplayName("Should delete subject successfully")
    void deleteSubject_ShouldDeleteSubject() {
        // Arrange
        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(subjectRepository.findByIdAndTenantId(SUBJECT_ID, TENANT_ID))
                .thenReturn(Optional.of(testSubject));
        doNothing().when(subjectRepository).delete(testSubject);

        // Act
        studyService.deleteSubject(SUBJECT_ID);

        // Assert
        verify(subjectRepository).findByIdAndTenantId(SUBJECT_ID, TENANT_ID);
        verify(subjectRepository).delete(testSubject);
    }

    @Test
    @DisplayName("Should log study session successfully")
    void logSession_ShouldCreateSession() {
        // Arrange
        StudySessionRequest request = new StudySessionRequest();
        request.setDurationMinutes(60);
        request.setNotes("Study notes");

        StudySession newSession = new StudySession();
        newSession.setDurationMinutes(60);

        StudySessionResponse response = new StudySessionResponse();
        response.setId(SESSION_ID);
        response.setDurationMinutes(60);

        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(subjectRepository.findByIdAndTenantId(SUBJECT_ID, TENANT_ID))
                .thenReturn(Optional.of(testSubject));
        when(mapper.toEntity(request)).thenReturn(newSession);
        when(studySessionRepository.save(newSession)).thenReturn(newSession);
        when(mapper.toResponse(newSession)).thenReturn(response);

        // Act
        StudySessionResponse result = studyService.logSession(SUBJECT_ID, request);

        // Assert
        assertNotNull(result);
        assertEquals(60, result.getDurationMinutes());
        verify(studySessionRepository).save(newSession);
    }

    @Test
    @DisplayName("Should get all sessions by subject")
    void getSessionsBySubject_ShouldReturnSessions() {
        // Arrange
        StudySessionResponse response = new StudySessionResponse();
        response.setId(SESSION_ID);
        response.setDurationMinutes(60);

        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(subjectRepository.findByIdAndTenantId(SUBJECT_ID, TENANT_ID))
                .thenReturn(Optional.of(testSubject));
        when(studySessionRepository.findBySubjectId(SUBJECT_ID))
                .thenReturn(Arrays.asList(testSession));
        when(mapper.toResponse(testSession)).thenReturn(response);

        // Act
        List<StudySessionResponse> result = studyService.getSessionsBySubject(SUBJECT_ID);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(60, result.get(0).getDurationMinutes());
    }

    @Test
    @DisplayName("Should delete session successfully")
    void deleteSession_ShouldDeleteSession() {
        // Arrange
        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(studySessionRepository.findByIdAndTenantId(SESSION_ID, TENANT_ID))
                .thenReturn(Optional.of(testSession));
        doNothing().when(studySessionRepository).delete(testSession);

        // Act
        studyService.deleteSession(SESSION_ID);

        // Assert
        verify(studySessionRepository).findByIdAndTenantId(SESSION_ID, TENANT_ID);
        verify(studySessionRepository).delete(testSession);
    }
}
