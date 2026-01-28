package com.matheusdev.mindforge.knowledgeltem.service;

import com.matheusdev.mindforge.core.tenant.context.TenantContext;
import com.matheusdev.mindforge.exception.ResourceNotFoundException;
import com.matheusdev.mindforge.knowledgeltem.model.KnowledgeItem;
import com.matheusdev.mindforge.knowledgeltem.model.KnowledgeVersion;
import com.matheusdev.mindforge.knowledgeltem.repository.KnowledgeItemRepository;
import com.matheusdev.mindforge.knowledgeltem.repository.KnowledgeVersionRepository;
import com.matheusdev.mindforge.workspace.model.Workspace;
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
class KnowledgeBaseServiceTest {

    @Mock
    private KnowledgeItemRepository repository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private KnowledgeVersionRepository versionRepository;

    @Mock
    private com.matheusdev.mindforge.knowledgeltem.mapper.KnowledgeItemMapper mapper;

    @InjectMocks
    private KnowledgeBaseService service;

    private MockedStatic<TenantContext> tenantContextMock;
    private KnowledgeItem testItem;
    private Workspace testWorkspace;
    private static final Long TENANT_ID = 1L;
    private static final Long ITEM_ID = 1L;
    private static final String WORKSPACE_ID = "1";

    @BeforeEach
    void setUp() {
        tenantContextMock = mockStatic(TenantContext.class);

        testWorkspace = new Workspace();
        testWorkspace.setId(Long.parseLong(WORKSPACE_ID));
        testWorkspace.setName("Knowledge Workspace");

        testItem = new KnowledgeItem();
        testItem.setId(ITEM_ID);
        testItem.setTitle("Test Item");
        testItem.setContent("Initial content");
        testItem.setWorkspace(testWorkspace);
        testItem.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        tenantContextMock.close();
    }

    @Test
    @DisplayName("Should return all items for tenant")
    void getAllKnowledgeItems_ShouldReturnList() {
        // Arrange
        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(repository.findByTenantId(TENANT_ID)).thenReturn(Arrays.asList(testItem));

        // Act
        List<KnowledgeItem> result = service.getAllKnowledgeItems();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Item", result.get(0).getTitle());
        verify(repository).findByTenantId(TENANT_ID);
    }

    @Test
    @DisplayName("Should return item by ID")
    void getItemById_ShouldReturnItem() {
        // Arrange
        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(repository.findByIdAndTenantId(ITEM_ID, TENANT_ID)).thenReturn(Optional.of(testItem));

        // Act
        KnowledgeItem result = service.getItemById(ITEM_ID);

        // Assert
        assertNotNull(result);
        assertEquals(ITEM_ID, result.getId());
        verify(repository).findByIdAndTenantId(ITEM_ID, TENANT_ID);
    }

    @Test
    @DisplayName("Should create item successfully")
    void createItem_ShouldSaveItem() {
        // Arrange
        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(workspaceRepository.findByIdAndTenantId(Long.parseLong(WORKSPACE_ID), TENANT_ID))
                .thenReturn(Optional.of(testWorkspace));
        when(repository.save(testItem)).thenReturn(testItem);

        // Act
        KnowledgeItem result = service.createItem(testItem, WORKSPACE_ID);

        // Assert
        assertNotNull(result);
        assertEquals(testWorkspace, result.getWorkspace());
        verify(repository).save(testItem);
    }

    @Test
    @DisplayName("Should update item successfully")
    void updateItem_ShouldUpdate() {
        // Arrange
        KnowledgeItem updatedData = new KnowledgeItem();
        updatedData.setTitle("Updated Title");
        updatedData.setContent("Updated content");

        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(repository.findByIdAndTenantId(ITEM_ID, TENANT_ID)).thenReturn(Optional.of(testItem));
        when(repository.save(testItem)).thenReturn(testItem);

        // Act
        KnowledgeItem result = service.updateItem(ITEM_ID, updatedData);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Title", testItem.getTitle());
        assertEquals("Updated content", testItem.getContent());
        verify(repository).save(testItem);
    }

    @Test
    @DisplayName("Should delete item successfully")
    void deleteItem_ShouldRemoveItem() {
        // Arrange
        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(repository.findByIdAndTenantId(ITEM_ID, TENANT_ID)).thenReturn(Optional.of(testItem));
        doNothing().when(repository).delete(testItem);

        // Act
        service.deleteItem(ITEM_ID);

        // Assert
        verify(repository).delete(testItem);
    }

    @Test
    @DisplayName("Should search items by tag")
    void searchByTag_ShouldReturnFilteredList() {
        // Arrange
        String tag = "java";
        testItem.setTags(Arrays.asList("java", "spring"));
        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(repository.findByTenantId(TENANT_ID)).thenReturn(Arrays.asList(testItem));

        // Act
        List<KnowledgeItem> result = service.searchByTag(tag);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should rollback to previous version")
    void rollbackToVersion_ShouldRestoreContent() {
        // Arrange
        Long versionId = 10L;
        KnowledgeVersion version = KnowledgeVersion.builder()
                .id(versionId)
                .knowledgeItemId(ITEM_ID)
                .title("Version Title")
                .content("Version content")
                .build();

        com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeItemResponse response = new com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeItemResponse();
        response.setId(ITEM_ID);

        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(repository.findByIdAndTenantId(ITEM_ID, TENANT_ID)).thenReturn(Optional.of(testItem));
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(version));
        when(repository.save(testItem)).thenReturn(testItem);
        when(mapper.toResponse(testItem)).thenReturn(response);

        // Act
        com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeItemResponse result = service.rollbackToVersion(ITEM_ID,
                versionId);

        // Assert
        assertNotNull(result);
        assertEquals(ITEM_ID, result.getId());
        verify(repository).save(testItem);
    }

    @Test
    @DisplayName("Should return version history")
    void getVersionHistory_ShouldReturnList() {
        // Arrange
        KnowledgeVersion v1 = KnowledgeVersion.builder()
                .id(1L)
                .knowledgeItemId(ITEM_ID)
                .content("Version 1 content")
                .title("V1")
                .build();
        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(repository.findByIdAndTenantId(ITEM_ID, TENANT_ID)).thenReturn(Optional.of(testItem));
        when(versionRepository.findByKnowledgeItemIdOrderByCreatedAtDesc(ITEM_ID))
                .thenReturn(Arrays.asList(v1));

        // Act
        List<com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeVersionResponse> result = service
                .getVersionHistory(ITEM_ID);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
