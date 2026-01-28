package com.matheusdev.mindforge.knowledgeltem.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeItemRequest;
import com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeItemResponse;
import com.matheusdev.mindforge.knowledgeltem.mapper.KnowledgeItemMapper;
import com.matheusdev.mindforge.knowledgeltem.model.KnowledgeItem;
import com.matheusdev.mindforge.knowledgeltem.service.KnowledgeBaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
class KnowledgeBaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KnowledgeBaseService service;

    @MockBean
    private KnowledgeItemMapper mapper;

    @Autowired
    private ObjectMapper objectMapper;

    private KnowledgeItem testItem;
    private KnowledgeItemResponse testResponse;
    private KnowledgeItemRequest testRequest;
    private static final Long ITEM_ID = 1L;

    @BeforeEach
    void setUp() {
        testItem = new KnowledgeItem();
        testItem.setId(ITEM_ID);
        testItem.setTitle("Test Item");

        testRequest = new KnowledgeItemRequest();
        testRequest.setTitle("Test Item");
        testRequest.setWorkspaceId("1");

        testResponse = new KnowledgeItemResponse();
        testResponse.setId(ITEM_ID);
        testResponse.setTitle("Test Item");
    }

    @Test
    @DisplayName("GET /api/knowledge should return all items")
    void getAllItems_ShouldReturnList() throws Exception {
        // Arrange
        when(service.getAllKnowledgeItems()).thenReturn(Arrays.asList(testItem));

        // Act & Assert
        mockMvc.perform(get("/api/knowledge")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(ITEM_ID));

        verify(service).getAllKnowledgeItems();
    }

    @Test
    @DisplayName("GET /api/knowledge/{id} should return item")
    void getItemById_ShouldReturnItem() throws Exception {
        // Arrange
        when(service.getItemById(ITEM_ID)).thenReturn(testItem);
        when(mapper.toResponse(testItem)).thenReturn(testResponse);

        // Act & Assert
        mockMvc.perform(get("/api/knowledge/{id}", ITEM_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ITEM_ID))
                .andExpect(jsonPath("$.title").value("Test Item"));

        verify(service).getItemById(ITEM_ID);
    }

    @Test
    @DisplayName("POST /api/knowledge should create item")
    void createItem_ShouldReturnCreatedItem() throws Exception {
        // Arrange
        when(mapper.toEntity(any(KnowledgeItemRequest.class))).thenReturn(testItem);
        when(service.createItem(any(KnowledgeItem.class), anyString())).thenReturn(testItem);
        when(mapper.toResponse(testItem)).thenReturn(testResponse);

        // Act & Assert
        mockMvc.perform(post("/api/knowledge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ITEM_ID));

        verify(service).createItem(any(KnowledgeItem.class), eq("1"));
    }

    @Test
    @DisplayName("PUT /api/knowledge/{id} should update item")
    void updateItem_ShouldReturnUpdatedItem() throws Exception {
        // Arrange
        when(mapper.toEntity(any(KnowledgeItemRequest.class))).thenReturn(testItem);
        when(service.updateItem(eq(ITEM_ID), any(KnowledgeItem.class))).thenReturn(testItem);
        when(mapper.toResponse(testItem)).thenReturn(testResponse);

        // Act & Assert
        mockMvc.perform(put("/api/knowledge/{id}", ITEM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ITEM_ID));

        verify(service).updateItem(eq(ITEM_ID), any(KnowledgeItem.class));
    }

    @Test
    @DisplayName("DELETE /api/knowledge/{id} should return no content")
    void deleteItem_ShouldReturnNoContent() throws Exception {
        // Arrange
        doNothing().when(service).deleteItem(ITEM_ID);

        // Act & Assert
        mockMvc.perform(delete("/api/knowledge/{id}", ITEM_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(service).deleteItem(ITEM_ID);
    }

    @Test
    @DisplayName("GET /api/knowledge/search should filter by tag")
    void searchByTag_ShouldReturnFilteredList() throws Exception {
        // Arrange
        String tag = "java";
        when(service.searchByTag(tag)).thenReturn(Arrays.asList(testItem));
        when(mapper.toResponse(testItem)).thenReturn(testResponse);

        // Act & Assert
        mockMvc.perform(get("/api/knowledge/search")
                .param("tag", tag)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(service).searchByTag(tag);
    }

    @Test
    @DisplayName("POST /api/knowledge/{id}/versions/{versionId}/rollback should rollback")
    void rollbackToVersion_ShouldReturnItem() throws Exception {
        // Arrange
        Long versionId = 2L;
        when(service.rollbackToVersion(ITEM_ID, versionId)).thenReturn(testResponse);
        when(mapper.toResponse(testItem)).thenReturn(testResponse);

        // Act & Assert
        mockMvc.perform(post("/api/knowledge/{id}/versions/{versionId}/rollback", ITEM_ID, versionId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ITEM_ID));

        verify(service).rollbackToVersion(ITEM_ID, versionId);
    }
}
