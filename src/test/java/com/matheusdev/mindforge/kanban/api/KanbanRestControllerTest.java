package com.matheusdev.mindforge.kanban.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusdev.mindforge.kanban.dto.KanbanColumnRequest;
import com.matheusdev.mindforge.kanban.dto.KanbanColumnResponse;
import com.matheusdev.mindforge.kanban.dto.KanbanTaskRequest;
import com.matheusdev.mindforge.kanban.dto.KanbanTaskResponse;
import com.matheusdev.mindforge.kanban.service.KanbanService;
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
class KanbanRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KanbanService service;

    @Autowired
    private ObjectMapper objectMapper;

    private KanbanColumnResponse testColumnResponse;
    private KanbanTaskResponse testTaskResponse;
    private static final Long COLUMN_ID = 1L;
    private static final Long TASK_ID = 1L;

    @BeforeEach
    void setUp() {
        testColumnResponse = new KanbanColumnResponse();
        testColumnResponse.setId(COLUMN_ID);
        testColumnResponse.setName("To Do");

        testTaskResponse = new KanbanTaskResponse();
        testTaskResponse.setId(TASK_ID);
        testTaskResponse.setTitle("Test Task");
    }

    @Test
    @DisplayName("GET /api/kanban/board should return board")
    void getBoard_ShouldReturnList() throws Exception {
        // Arrange
        when(service.getBoard()).thenReturn(Arrays.asList(testColumnResponse));

        // Act & Assert
        mockMvc.perform(get("/api/kanban/board")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(COLUMN_ID));

        verify(service).getBoard();
    }

    @Test
    @DisplayName("POST /api/kanban/columns should create column")
    void createColumn_ShouldReturnCreatedColumn() throws Exception {
        // Arrange
        KanbanColumnRequest request = new KanbanColumnRequest();
        request.setName("New Column");

        when(service.createColumn(any(KanbanColumnRequest.class))).thenReturn(testColumnResponse);

        // Act & Assert
        mockMvc.perform(post("/api/kanban/columns")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(COLUMN_ID));

        verify(service).createColumn(any(KanbanColumnRequest.class));
    }

    @Test
    @DisplayName("POST /api/kanban/columns/{columnId}/tasks should create task")
    void createTask_ShouldReturnCreatedTask() throws Exception {
        // Arrange
        KanbanTaskRequest request = new KanbanTaskRequest();
        request.setTitle("New Task");

        when(service.createTask(eq(COLUMN_ID), any(KanbanTaskRequest.class))).thenReturn(testTaskResponse);

        // Act & Assert
        mockMvc.perform(post("/api/kanban/columns/{columnId}/tasks", COLUMN_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TASK_ID));

        verify(service).createTask(eq(COLUMN_ID), any(KanbanTaskRequest.class));
    }

    @Test
    @DisplayName("PUT /api/kanban/tasks/{taskId}/move/{targetColumnId} should move task")
    void moveTask_ShouldReturnUpdatedTask() throws Exception {
        // Arrange
        Long targetColumnId = 2L;
        when(service.moveTask(TASK_ID, targetColumnId)).thenReturn(testTaskResponse);

        // Act & Assert
        mockMvc.perform(put("/api/kanban/tasks/{taskId}/move/{targetColumnId}", TASK_ID, targetColumnId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TASK_ID));

        verify(service).moveTask(TASK_ID, targetColumnId);
    }

    @Test
    @DisplayName("DELETE /api/kanban/tasks/{taskId} should return no content")
    void deleteTask_ShouldReturnNoContent() throws Exception {
        // Arrange
        doNothing().when(service).deleteTask(TASK_ID);

        // Act & Assert
        mockMvc.perform(delete("/api/kanban/tasks/{taskId}", TASK_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(service).deleteTask(TASK_ID);
    }
}
