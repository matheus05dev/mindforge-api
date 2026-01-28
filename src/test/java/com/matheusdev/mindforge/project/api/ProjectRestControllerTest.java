package com.matheusdev.mindforge.project.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusdev.mindforge.project.dto.LinkRepositoryRequest;
import com.matheusdev.mindforge.project.dto.ProjectRequest;
import com.matheusdev.mindforge.project.dto.ProjectResponse;
import com.matheusdev.mindforge.project.milestone.dto.MilestoneRequest;
import com.matheusdev.mindforge.project.milestone.dto.MilestoneResponse;
import com.matheusdev.mindforge.project.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class ProjectRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectService projectService;

    @Autowired
    private ObjectMapper objectMapper;

    private ProjectResponse testResponse;
    private ProjectRequest testRequest;
    private static final Long PROJECT_ID = 1L;

    @BeforeEach
    void setUp() {
        testRequest = new ProjectRequest();
        testRequest.setName("Test Project");
        testRequest.setWorkspaceId(1L);

        testResponse = new ProjectResponse();
        testResponse.setId(PROJECT_ID);
        testResponse.setName("Test Project");
    }

    @Test
    @DisplayName("GET /api/projects should return paginated projects")
    void getAllProjects_ShouldReturnPage() throws Exception {
        // Arrange
        Page<ProjectResponse> page = new PageImpl<>(Arrays.asList(testResponse));
        when(projectService.getAllProjects(any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/projects")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(PROJECT_ID))
                .andExpect(jsonPath("$.content[0].name").value("Test Project"));

        verify(projectService).getAllProjects(any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/projects/{id} should return project")
    void getProjectById_ShouldReturnProject() throws Exception {
        // Arrange
        when(projectService.getProjectById(PROJECT_ID)).thenReturn(testResponse);

        // Act & Assert
        mockMvc.perform(get("/api/projects/{id}", PROJECT_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PROJECT_ID))
                .andExpect(jsonPath("$.name").value("Test Project"));

        verify(projectService).getProjectById(PROJECT_ID);
    }

    @Test
    @DisplayName("POST /api/projects should create project")
    void createProject_ShouldReturnCreatedProject() throws Exception {
        // Arrange
        when(projectService.createProject(any(ProjectRequest.class))).thenReturn(testResponse);

        // Act & Assert
        mockMvc.perform(post("/api/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PROJECT_ID));

        verify(projectService).createProject(any(ProjectRequest.class));
    }

    @Test
    @DisplayName("PUT /api/projects/{id} should update project")
    void updateProject_ShouldReturnUpdatedProject() throws Exception {
        // Arrange
        when(projectService.updateProject(eq(PROJECT_ID), any(ProjectRequest.class))).thenReturn(testResponse);

        // Act & Assert
        mockMvc.perform(put("/api/projects/{id}", PROJECT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PROJECT_ID));

        verify(projectService).updateProject(eq(PROJECT_ID), any(ProjectRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/projects/{id} should return no content")
    void deleteProject_ShouldReturnNoContent() throws Exception {
        // Arrange
        doNothing().when(projectService).deleteProject(PROJECT_ID);

        // Act & Assert
        mockMvc.perform(delete("/api/projects/{id}", PROJECT_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(projectService).deleteProject(PROJECT_ID);
    }

    @Test
    @DisplayName("POST /api/projects/{id}/link should link repo")
    void linkRepository_ShouldReturnProject() throws Exception {
        // Arrange
        LinkRepositoryRequest linkRequest = new LinkRepositoryRequest();
        linkRequest.setRepoUrl("https://github.com/user/repo");

        when(projectService.linkRepository(eq(PROJECT_ID), anyString())).thenReturn(testResponse);

        // Act & Assert
        mockMvc.perform(post("/api/projects/{id}/link", PROJECT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(linkRequest)))
                .andExpect(status().isOk());

        verify(projectService).linkRepository(eq(PROJECT_ID), eq("https://github.com/user/repo"));
    }

    @Test
    @DisplayName("GET /api/projects/{id}/milestones should return list")
    void getMilestonesByProject_ShouldReturnList() throws Exception {
        // Arrange
        MilestoneResponse milestone = new MilestoneResponse();
        milestone.setId(1L);
        List<MilestoneResponse> list = Arrays.asList(milestone);

        when(projectService.getMilestonesByProject(PROJECT_ID)).thenReturn(list);

        // Act & Assert
        mockMvc.perform(get("/api/projects/{id}/milestones", PROJECT_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(projectService).getMilestonesByProject(PROJECT_ID);
    }

    @Test
    @DisplayName("POST /api/projects/{projectId}/milestones should add milestone")
    void addMilestone_ShouldReturnCreatedMilestone() throws Exception {
        // Arrange
        MilestoneRequest milestoneRequest = new MilestoneRequest();
        milestoneRequest.setTitle("New Milestone");

        MilestoneResponse milestoneResponse = new MilestoneResponse();
        milestoneResponse.setId(1L);

        when(projectService.addMilestone(eq(PROJECT_ID), any(MilestoneRequest.class))).thenReturn(milestoneResponse);

        // Act & Assert
        mockMvc.perform(post("/api/projects/{projectId}/milestones", PROJECT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(milestoneRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(projectService).addMilestone(eq(PROJECT_ID), any(MilestoneRequest.class));
    }
}
