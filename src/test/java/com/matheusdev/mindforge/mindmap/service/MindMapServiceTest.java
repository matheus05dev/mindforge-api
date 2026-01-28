package com.matheusdev.mindforge.mindmap.service;

import com.matheusdev.mindforge.core.tenant.context.TenantContext;
import com.matheusdev.mindforge.mindmap.model.MindMap;
import com.matheusdev.mindforge.mindmap.repository.MindMapRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MindMapServiceTest {

    @Mock
    private MindMapRepository repository;

    @InjectMocks
    private MindMapService service;

    private MockedStatic<TenantContext> tenantContextMock;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        tenantContextMock = mockStatic(TenantContext.class);
    }

    @AfterEach
    void tearDown() {
        tenantContextMock.close();
    }

    @Test
    @DisplayName("Should return existing Mind Map")
    void getMindMap_ShouldReturnExisting() {
        // Arrange
        MindMap existing = MindMap.builder().name("Geral").nodesJson("[]").edgesJson("[]").build();
        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(repository.findByNameAndTenantId("Geral", TENANT_ID)).thenReturn(Optional.of(existing));

        // Act
        MindMap result = service.getMindMap();

        // Assert
        assertNotNull(result);
        assertEquals("Geral", result.getName());
        verify(repository).findByNameAndTenantId("Geral", TENANT_ID);
    }

    @Test
    @DisplayName("Should return new Mind Map if not found")
    void getMindMap_ShouldReturnNew_WhenNotFound() {
        // Arrange
        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(repository.findByNameAndTenantId("Geral", TENANT_ID)).thenReturn(Optional.empty());

        // Act
        MindMap result = service.getMindMap();

        // Assert
        assertNotNull(result);
        assertEquals("Geral", result.getName());
        assertEquals("[]", result.getNodesJson());
    }

    @Test
    @DisplayName("Should save Mind Map successfully")
    void saveMindMap_ShouldUpdateAndSave() {
        // Arrange
        String nodes = "[{\"id\":\"1\"}]";
        String edges = "[]";
        MindMap existing = MindMap.builder().name("Geral").build();

        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(repository.findByNameAndTenantId("Geral", TENANT_ID)).thenReturn(Optional.of(existing));
        when(repository.save(any(MindMap.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        MindMap result = service.saveMindMap(nodes, edges);

        // Assert
        assertNotNull(result);
        assertEquals(nodes, result.getNodesJson());
        assertEquals(edges, result.getEdgesJson());
        verify(repository).save(any(MindMap.class));
    }
}
