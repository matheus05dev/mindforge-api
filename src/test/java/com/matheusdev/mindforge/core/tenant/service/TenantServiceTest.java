package com.matheusdev.mindforge.core.tenant.service;

import com.matheusdev.mindforge.core.tenant.model.Tenant;
import com.matheusdev.mindforge.core.tenant.model.TenantPlan;
import com.matheusdev.mindforge.core.tenant.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private TenantService tenantService;

    private Tenant testTenant;
    private static final Long TENANT_ID = 1L;
    private static final String TENANT_SLUG = "test-tenant";

    @BeforeEach
    void setUp() {
        testTenant = Tenant.builder()
                .id(TENANT_ID)
                .name("Test Tenant")
                .slug(TENANT_SLUG)
                .active(true)
                .plan(TenantPlan.FREE)
                .maxUsers(5)
                .build();
    }

    @Test
    @DisplayName("Should return tenant by ID when it exists")
    void getTenantById_ShouldReturnTenant_WhenTenantExists() {
        // Arrange
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(testTenant));

        // Act
        Tenant result = tenantService.getTenantById(TENANT_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TENANT_ID, result.getId());
        assertEquals("Test Tenant", result.getName());
        assertEquals(TENANT_SLUG, result.getSlug());
        verify(tenantRepository).findById(TENANT_ID);
    }

    @Test
    @DisplayName("Should throw exception when tenant not found by ID")
    void getTenantById_ShouldThrowException_WhenTenantNotFound() {
        // Arrange
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tenantService.getTenantById(TENANT_ID));
        assertTrue(exception.getMessage().contains("Tenant not found"));
        verify(tenantRepository).findById(TENANT_ID);
    }

    @Test
    @DisplayName("Should return tenant by slug when it exists")
    void getTenantBySlug_ShouldReturnTenant_WhenTenantExists() {
        // Arrange
        when(tenantRepository.findBySlug(TENANT_SLUG)).thenReturn(Optional.of(testTenant));

        // Act
        Tenant result = tenantService.getTenantBySlug(TENANT_SLUG);

        // Assert
        assertNotNull(result);
        assertEquals(TENANT_SLUG, result.getSlug());
        assertEquals("Test Tenant", result.getName());
        verify(tenantRepository).findBySlug(TENANT_SLUG);
    }

    @Test
    @DisplayName("Should throw exception when tenant not found by slug")
    void getTenantBySlug_ShouldThrowException_WhenTenantNotFound() {
        // Arrange
        when(tenantRepository.findBySlug(TENANT_SLUG)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tenantService.getTenantBySlug(TENANT_SLUG));
        assertTrue(exception.getMessage().contains("Tenant not found"));
        verify(tenantRepository).findBySlug(TENANT_SLUG);
    }

    @Test
    @DisplayName("Should create tenant successfully")
    void createTenant_ShouldSaveAndReturnTenant_WhenSlugIsUnique() {
        // Arrange
        Tenant newTenant = Tenant.builder()
                .name("New Tenant")
                .slug("new-tenant")
                .active(true)
                .plan(TenantPlan.PRO)
                .maxUsers(10)
                .build();

        when(tenantRepository.existsBySlug("new-tenant")).thenReturn(false);
        when(tenantRepository.save(newTenant)).thenReturn(newTenant);

        // Act
        Tenant result = tenantService.createTenant(newTenant);

        // Assert
        assertNotNull(result);
        assertEquals("New Tenant", result.getName());
        assertEquals("new-tenant", result.getSlug());
        verify(tenantRepository).existsBySlug("new-tenant");
        verify(tenantRepository).save(newTenant);
    }

    @Test
    @DisplayName("Should throw exception when creating tenant with duplicate slug")
    void createTenant_ShouldThrowException_WhenSlugAlreadyExists() {
        // Arrange
        Tenant newTenant = Tenant.builder()
                .name("Duplicate Tenant")
                .slug(TENANT_SLUG)
                .build();

        when(tenantRepository.existsBySlug(TENANT_SLUG)).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tenantService.createTenant(newTenant));
        assertTrue(exception.getMessage().contains("already exists"));
        verify(tenantRepository).existsBySlug(TENANT_SLUG);
        verify(tenantRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update tenant successfully")
    void updateTenant_ShouldUpdateAndReturnTenant() {
        // Arrange
        Tenant updateData = Tenant.builder()
                .name("Updated Tenant")
                .active(false)
                .plan(TenantPlan.PRO)
                .maxUsers(20)
                .build();

        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(testTenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(testTenant);

        // Act
        Tenant result = tenantService.updateTenant(TENANT_ID, updateData);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Tenant", result.getName());
        assertEquals(false, result.getActive());
        assertEquals(TenantPlan.PRO, result.getPlan());
        assertEquals(20, result.getMaxUsers());
        verify(tenantRepository).findById(TENANT_ID);
        verify(tenantRepository).save(testTenant);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent tenant")
    void updateTenant_ShouldThrowException_WhenTenantNotFound() {
        // Arrange
        Tenant updateData = Tenant.builder().name("Updated").build();
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> tenantService.updateTenant(TENANT_ID, updateData));
        verify(tenantRepository).findById(TENANT_ID);
        verify(tenantRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete tenant successfully")
    void deleteTenant_ShouldDeleteTenant_WhenNotDefaultTenant() {
        // Arrange
        Tenant tenantToDelete = Tenant.builder()
                .id(2L)
                .name("Deletable Tenant")
                .slug("deletable")
                .build();

        when(tenantRepository.findById(2L)).thenReturn(Optional.of(tenantToDelete));
        doNothing().when(tenantRepository).delete(tenantToDelete);

        // Act
        tenantService.deleteTenant(2L);

        // Assert
        verify(tenantRepository).findById(2L);
        verify(tenantRepository).delete(tenantToDelete);
    }

    @Test
    @DisplayName("Should throw exception when deleting default tenant")
    void deleteTenant_ShouldThrowException_WhenDeletingDefaultTenant() {
        // Arrange
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(testTenant));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> tenantService.deleteTenant(1L));
        assertTrue(exception.getMessage().contains("Cannot delete default tenant"));
        verify(tenantRepository).findById(1L);
        verify(tenantRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent tenant")
    void deleteTenant_ShouldThrowException_WhenTenantNotFound() {
        // Arrange
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> tenantService.deleteTenant(TENANT_ID));
        verify(tenantRepository).findById(TENANT_ID);
        verify(tenantRepository, never()).delete(any());
    }
}
