package com.matheusdev.mindforge.project.service;

import com.matheusdev.mindforge.core.tenant.context.TenantContext;
import com.matheusdev.mindforge.exception.BusinessException;
import com.matheusdev.mindforge.project.dto.ProjectRequest;
import com.matheusdev.mindforge.project.dto.ProjectResponse;
import com.matheusdev.mindforge.project.dto.ProjectSummaryResponse;
import com.matheusdev.mindforge.project.mapper.ProjectMapper;
import com.matheusdev.mindforge.project.milestone.dto.MilestoneRequest;
import com.matheusdev.mindforge.project.milestone.dto.MilestoneResponse;
import com.matheusdev.mindforge.project.milestone.model.Milestone;
import com.matheusdev.mindforge.project.milestone.repository.MilestoneRepository;
import com.matheusdev.mindforge.project.model.Project;
import com.matheusdev.mindforge.project.repository.ProjectRepository;
import com.matheusdev.mindforge.workspace.model.Workspace;
import com.matheusdev.mindforge.workspace.model.WorkspaceType;
import com.matheusdev.mindforge.workspace.service.WorkspaceService;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private ProjectMapper mapper;

    @InjectMocks
    private ProjectService projectService;

    private MockedStatic<TenantContext> tenantContextMock;
    private Project testProject;
    private Workspace testWorkspace;
    private static final Long TENANT_ID = 1L;
    private static final Long PROJECT_ID = 1L;
    private static final Long WORKSPACE_ID = 1L;

    @BeforeEach
    void setUp() {
        tenantContextMock = mockStatic(TenantContext.class);

        testWorkspace = new Workspace();
        testWorkspace.setId(WORKSPACE_ID);
        testWorkspace.setName("Project Workspace");
        testWorkspace.setType(WorkspaceType.PROJECT);

        testProject = new Project();
        testProject.setId(PROJECT_ID);
        testProject.setName("Test Project");
        testProject.setWorkspace(testWorkspace);
        testProject.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        tenantContextMock.close();
    }

    @Test
    @DisplayName("Should return all projects for tenant")
    void getAllProjects_ShouldReturnProjects() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Project> projectPage = new PageImpl<>(Arrays.asList(testProject));
        ProjectResponse response = new ProjectResponse();
        response.setId(PROJECT_ID);
        response.setName("Test Project");

        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(projectRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(projectPage);
        when(mapper.toResponse(testProject)).thenReturn(response);

        // Act
        Page<ProjectResponse> result = projectService.getAllProjects(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test Project", result.getContent().get(0).getName());
        verify(projectRepository).findByTenantId(TENANT_ID, pageable);
    }

    @Test
    @DisplayName("Should throw exception when tenant context not set in getAllProjects")
    void getAllProjects_ShouldThrowException_WhenTenantContextNotSet() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        tenantContextMock.when(TenantContext::getTenantId).thenReturn(null);

        // Act & Assert
        assertThrows(BusinessException.class, () -> projectService.getAllProjects(pageable));
    }

    @Test
    @DisplayName("Should return projects by workspace ID")
    void getProjectsByWorkspaceId_ShouldReturnProjects() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Project> projectPage = new PageImpl<>(Arrays.asList(testProject));
        ProjectSummaryResponse response = new ProjectSummaryResponse();
        response.setId(PROJECT_ID);
        response.setName("Test Project");

        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(workspaceService.findById(WORKSPACE_ID)).thenReturn(testWorkspace);
        when(projectRepository.findByWorkspaceIdAndTenantId(WORKSPACE_ID, TENANT_ID, pageable)).thenReturn(projectPage);
        when(mapper.toSummaryResponse(testProject)).thenReturn(response);

        // Act
        Page<ProjectSummaryResponse> result = projectService.getProjectsByWorkspaceId(WORKSPACE_ID, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(projectRepository).findByWorkspaceIdAndTenantId(WORKSPACE_ID, TENANT_ID, pageable);
    }

    @Test
    @DisplayName("Should return project by ID")
    void getProjectById_ShouldReturnProject() {
        // Arrange
        ProjectResponse response = new ProjectResponse();
        response.setId(PROJECT_ID);

        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(testProject));
        when(mapper.toResponse(testProject)).thenReturn(response);

        // Act
        ProjectResponse result = projectService.getProjectById(PROJECT_ID);

        // Assert
        assertNotNull(result);
        assertEquals(PROJECT_ID, result.getId());
        verify(projectRepository).findById(PROJECT_ID);
    }

    @Test
    @DisplayName("Should create project when workspace type is PROJECT")
    void createProject_ShouldCreateProject_WhenWorkspaceTypeIsProject() {
        // Arrange
        ProjectRequest request = new ProjectRequest();
        request.setName("New Project");
        request.setWorkspaceId(WORKSPACE_ID);

        Project newProject = new Project();
        newProject.setName("New Project");

        ProjectResponse response = new ProjectResponse();
        response.setName("New Project");

        when(workspaceService.findById(WORKSPACE_ID)).thenReturn(testWorkspace);
        when(mapper.toEntity(request)).thenReturn(newProject);
        when(projectRepository.save(newProject)).thenReturn(newProject);
        when(mapper.toResponse(newProject)).thenReturn(response);

        // Act
        ProjectResponse result = projectService.createProject(request);

        // Assert
        assertNotNull(result);
        assertEquals("New Project", result.getName());
        verify(projectRepository).save(newProject);
    }

    @Test
    @DisplayName("Should throw exception when creating project in invalid workspace type")
    void createProject_ShouldThrowException_WhenWorkspaceTypeIsInvalid() {
        // Arrange
        testWorkspace.setType(WorkspaceType.STUDY);
        ProjectRequest request = new ProjectRequest();
        request.setWorkspaceId(WORKSPACE_ID);

        when(workspaceService.findById(WORKSPACE_ID)).thenReturn(testWorkspace);

        // Act & Assert
        assertThrows(BusinessException.class, () -> projectService.createProject(request));
    }

    @Test
    @DisplayName("Should update project successfully")
    void updateProject_ShouldUpdateProject() {
        // Arrange
        ProjectRequest request = new ProjectRequest();
        request.setName("Updated Project");

        ProjectResponse response = new ProjectResponse();
        response.setName("Updated Project");

        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(testProject));
        doNothing().when(mapper).updateProjectFromRequest(request, testProject);
        when(projectRepository.save(testProject)).thenReturn(testProject);
        when(mapper.toResponse(testProject)).thenReturn(response);

        // Act
        ProjectResponse result = projectService.updateProject(PROJECT_ID, request);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Project", result.getName());
        verify(projectRepository).save(testProject);
    }

    @Test
    @DisplayName("Should delete project successfully")
    void deleteProject_ShouldDeleteProject() {
        // Arrange
        when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
        doNothing().when(projectRepository).deleteById(PROJECT_ID);

        // Act
        projectService.deleteProject(PROJECT_ID);

        // Assert
        verify(projectRepository).deleteById(PROJECT_ID);
    }

    @Test
    @DisplayName("Should link repository successfully")
    void linkRepository_ShouldUpdateRepoUrl() {
        // Arrange
        String repoUrl = "https://github.com/user/repo";
        ProjectResponse response = new ProjectResponse();
        response.setGithubRepoUrl(repoUrl);

        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(testProject));
        when(projectRepository.save(testProject)).thenReturn(testProject);
        when(mapper.toResponse(testProject)).thenReturn(response);

        // Act
        ProjectResponse result = projectService.linkRepository(PROJECT_ID, repoUrl);

        // Assert
        assertNotNull(result);
        assertEquals(repoUrl, result.getGithubRepoUrl());
        assertEquals(repoUrl, testProject.getGithubRepoUrl());
    }

    @Test
    @DisplayName("Should add milestone successfully")
    void addMilestone_ShouldCreateMilestone() {
        // Arrange
        MilestoneRequest request = new MilestoneRequest();
        Milestone milestone = new Milestone();
        MilestoneResponse response = new MilestoneResponse();

        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(testProject));
        when(mapper.toEntity(request)).thenReturn(milestone);
        when(milestoneRepository.save(milestone)).thenReturn(milestone);
        when(mapper.toResponse(milestone)).thenReturn(response);

        // Act
        MilestoneResponse result = projectService.addMilestone(PROJECT_ID, request);

        // Assert
        assertNotNull(result);
        verify(milestoneRepository).save(milestone);
        assertEquals(testProject, milestone.getProject());
    }

    @Test
    @DisplayName("Should return milestones by project")
    void getMilestonesByProject_ShouldReturnList() {
        // Arrange
        Milestone milestone = new Milestone();
        MilestoneResponse response = new MilestoneResponse();

        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(testProject));
        when(milestoneRepository.findByProjectId(PROJECT_ID)).thenReturn(Arrays.asList(milestone));
        when(mapper.toResponse(milestone)).thenReturn(response);

        // Act
        List<MilestoneResponse> result = projectService.getMilestonesByProject(PROJECT_ID);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
