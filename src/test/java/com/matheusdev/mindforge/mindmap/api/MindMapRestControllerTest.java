package com.matheusdev.mindforge.mindmap.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusdev.mindforge.mindmap.dto.SaveMindMapRequest;
import com.matheusdev.mindforge.mindmap.model.MindMap;
import com.matheusdev.mindforge.mindmap.service.MindMapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class MindMapRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MindMapService service;

    @Autowired
    private ObjectMapper objectMapper;

    private MindMap testMindMap;
    private static final Long MAP_ID = 1L;

    @BeforeEach
    void setUp() {
        testMindMap = MindMap.builder()
                .id(MAP_ID)
                .name("Geral")
                .nodesJson("[]")
                .edgesJson("[]")
                .build();
    }

    @Test
    @DisplayName("GET /api/mind-map should return current map")
    void getMindMap_ShouldReturnMap() throws Exception {
        // Arrange
        when(service.getMindMap()).thenReturn(testMindMap);

        // Act & Assert
        mockMvc.perform(get("/api/mind-map")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(MAP_ID))
                .andExpect(jsonPath("$.name").value("Geral"));

        verify(service).getMindMap();
    }

    @Test
    @DisplayName("POST /api/mind-map should save map")
    void saveMindMap_ShouldReturnSavedMap() throws Exception {
        // Arrange
        SaveMindMapRequest request = new SaveMindMapRequest();
        request.setNodesJson("[{\"id\":\"1\"}]");
        request.setEdgesJson("[]");

        testMindMap.setNodesJson(request.getNodesJson());

        when(service.saveMindMap(anyString(), anyString())).thenReturn(testMindMap);

        // Act & Assert
        mockMvc.perform(post("/api/mind-map")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodesJson").value(request.getNodesJson()));

        verify(service).saveMindMap(eq(request.getNodesJson()), eq(request.getEdgesJson()));
    }
}
