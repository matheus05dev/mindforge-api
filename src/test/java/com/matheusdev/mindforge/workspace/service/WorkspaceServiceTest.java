package com.matheusdev.mindforge.workspace.service;

import com.matheusdev.mindforge.core.tenant.context.TenantContext;
import com.matheusdev.mindforge.exception.ResourceNotFoundException;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @InjectMocks
    private WorkspaceService workspaceService;

    private MockedStatic<TenantContext> tenantContextMock;
    private Workspace testWorkspace;
    private static final Long TENANT_ID = 1L;
    private static final Long WORKSPACE_ID = 1L;

    @BeforeEach
    void setUp() {
        tenantContextMock = mockStatic(TenantContext.class);

        testWorkspace = new Workspace();
        testWorkspace.setId(WORKSPACE_ID);
        testWorkspace.setName("Test Workspace");
        testWorkspace.setDescription("Test Description");
        testWorkspace.setType(WorkspaceType.GENERIC);
        testWorkspace.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        tenantContextMock.close();
    }

    @Test
    @DisplayName("Should return all workspaces for tenant")
    void findAll_ShouldReturnWorkspaces_WhenTenantContextIsSet() {
        // Arrange
        Workspace workspace2 = new Workspace();
        workspace2.setId(2L);
        workspace2.setName("Workspace 2");
        workspace2.setType(WorkspaceType.PROJECT);

        List<Workspace> workspaces = Arrays.asList(testWorkspace, workspace2);

        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(workspaceRepository.findByTenantId(TENANT_ID)).thenReturn(workspaces);

        // Act
        List<Workspace> result = workspaceService.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Test Workspace", result.get(0).getName());
        assertEquals("Workspace 2", result.get(1).getName());
        verify(workspaceRepository).findByTenantId(TENANT_ID);
    }

    @Test
    @DisplayName("Should throw exception when tenant context is not set")
    void findAll_ShouldThrowException_WhenTenantContextIsNull() {
        // Arrange
        tenantContextMock.when(TenantContext::getTenantId).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> workspaceService.findAll());
        assertEquals("Tenant context not set", exception.getMessage());
        verify(workspaceRepository, never()).findByTenantId(any());
    }

    @Test
    @DisplayName("Should return workspace by ID when it exists")
    void findById_ShouldReturnWorkspace_WhenWorkspaceExists() {
        // Arrange
        when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.of(testWorkspace));

        // Act
        Workspace result = workspaceService.findById(WORKSPACE_ID);

        // Assert
        assertNotNull(result);
        assertEquals(WORKSPACE_ID, result.getId());
        assertEquals("Test Workspace", result.getName());
        verify(workspaceRepository).findById(WORKSPACE_ID);
    }

    @Test
    @DisplayName("Should throw exception when workspace not found")
    void findById_ShouldThrowException_WhenWorkspaceNotFound() {
        // Arrange
        when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> workspaceService.findById(WORKSPACE_ID));
        assertTrue(exception.getMessage().contains("Workspace não encontrado"));
        verify(workspaceRepository).findById(WORKSPACE_ID);
    }

    @Test
    @DisplayName("Should create workspace successfully")
    void create_ShouldSaveAndReturnWorkspace() {
        // Arrange
        Workspace newWorkspace = new Workspace();
        newWorkspace.setName("New Workspace");
        newWorkspace.setType(WorkspaceType.STUDY);

        Workspace savedWorkspace = new Workspace();
        savedWorkspace.setId(2L);
        savedWorkspace.setName("New Workspace");
        savedWorkspace.setType(WorkspaceType.STUDY);

        when(workspaceRepository.save(newWorkspace)).thenReturn(savedWorkspace);

        // Act
        Workspace result = workspaceService.create(newWorkspace);

        // Assert
        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("New Workspace", result.getName());
        verify(workspaceRepository).save(newWorkspace);
    }

    @Test
    @DisplayName("Should update workspace successfully")
    void update_ShouldUpdateAndReturnWorkspace() {
        // Arrange
        Workspace updateData = new Workspace();
        updateData.setName("Updated Name");
        updateData.setDescription("Updated Description");
        updateData.setType(WorkspaceType.PROJECT);

        when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.of(testWorkspace));
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(testWorkspace);

        // Act
        Workspace result = workspaceService.update(WORKSPACE_ID, updateData);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Name", result.getName());
        assertEquals("Updated Description", result.getDescription());
        assertEquals(WorkspaceType.PROJECT, result.getType());
        verify(workspaceRepository).findById(WORKSPACE_ID);
        verify(workspaceRepository).save(testWorkspace);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent workspace")
    void update_ShouldThrowException_WhenWorkspaceNotFound() {
        // Arrange
        Workspace updateData = new Workspace();
        updateData.setName("Updated Name");

        when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> workspaceService.update(WORKSPACE_ID, updateData));
        verify(workspaceRepository).findById(WORKSPACE_ID);
        verify(workspaceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete workspace successfully")
    void delete_ShouldDeleteWorkspace_WhenWorkspaceExists() {
        // Arrange
        when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.of(testWorkspace));
        doNothing().when(workspaceRepository).delete(testWorkspace);

        // Act
        workspaceService.delete(WORKSPACE_ID);

        // Assert
        verify(workspaceRepository).findById(WORKSPACE_ID);
        verify(workspaceRepository).delete(testWorkspace);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent workspace")
    void delete_ShouldThrowException_WhenWorkspaceNotFound() {
        // Arrange
        when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> workspaceService.delete(WORKSPACE_ID));
        verify(workspaceRepository).findById(WORKSPACE_ID);
        verify(workspaceRepository, never()).delete(any());
    }
}
