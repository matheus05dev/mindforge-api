package com.matheusdev.mindforge.kanban.service;

import com.matheusdev.mindforge.core.tenant.context.TenantContext;
import com.matheusdev.mindforge.kanban.dto.KanbanColumnRequest;
import com.matheusdev.mindforge.kanban.dto.KanbanColumnResponse;
import com.matheusdev.mindforge.kanban.dto.KanbanTaskRequest;
import com.matheusdev.mindforge.kanban.dto.KanbanTaskResponse;
import com.matheusdev.mindforge.kanban.mapper.KanbanMapper;
import com.matheusdev.mindforge.kanban.model.KanbanBoard;
import com.matheusdev.mindforge.kanban.model.KanbanColumn;
import com.matheusdev.mindforge.kanban.model.KanbanTask;
import com.matheusdev.mindforge.kanban.repository.KanbanColumnRepository;
import com.matheusdev.mindforge.kanban.repository.KanbanTaskRepository;
import com.matheusdev.mindforge.study.subject.repository.SubjectRepository;
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
class KanbanServiceTest {

    @Mock
    private KanbanColumnRepository columnRepository;

    @Mock
    private KanbanTaskRepository taskRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private KanbanMapper mapper;

    @InjectMocks
    private KanbanService service;

    private MockedStatic<TenantContext> tenantContextMock;
    private KanbanColumn testColumn;
    private KanbanTask testTask;
    private static final Long TENANT_ID = 1L;
    private static final Long COLUMN_ID = 1L;
    private static final Long TASK_ID = 1L;

    @BeforeEach
    void setUp() {
        tenantContextMock = mockStatic(TenantContext.class);

        testColumn = new KanbanColumn();
        testColumn.setId(COLUMN_ID);
        testColumn.setName("To Do");
        // Board provides the tenant context for columns
        KanbanBoard board = new KanbanBoard();
        board.setId(1L);
        com.matheusdev.mindforge.core.tenant.model.Tenant tenant = new com.matheusdev.mindforge.core.tenant.model.Tenant();
        tenant.setId(TENANT_ID);
        board.setTenant(tenant);
        board.setTenantId(TENANT_ID); // Set tenantId directly to avoid NPE in service check
        testColumn.setBoard(board);

        testTask = new KanbanTask();
        testTask.setId(TASK_ID);
        testTask.setTitle("Test Task");
        testTask.setColumn(testColumn);
    }

    @AfterEach
    void tearDown() {
        tenantContextMock.close();
    }

    @Test
    @DisplayName("Should return board columns")
    void getBoard_ShouldReturnColumns() {
        // Arrange
        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(columnRepository.findAllByTenantId(TENANT_ID)).thenReturn(Arrays.asList(testColumn));
        when(mapper.toResponse(testColumn)).thenReturn(new KanbanColumnResponse());

        // Act
        List<KanbanColumnResponse> result = service.getBoard();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(columnRepository).findAllByTenantId(TENANT_ID);
    }

    @Test
    @DisplayName("Should create column successfully")
    void createColumn_ShouldSaveColumn() {
        // Arrange
        KanbanColumnRequest request = new KanbanColumnRequest();
        request.setName("New Column");

        when(mapper.toEntity(request)).thenReturn(testColumn);
        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(columnRepository.save(testColumn)).thenReturn(testColumn);
        when(mapper.toResponse(testColumn)).thenReturn(new KanbanColumnResponse());

        // Act
        KanbanColumnResponse result = service.createColumn(request);

        // Assert
        assertNotNull(result);
        verify(columnRepository).save(testColumn);
    }

    @Test
    @DisplayName("Should create task in column")
    void createTask_ShouldSaveTaskInColumn() {
        // Arrange
        KanbanTaskRequest request = new KanbanTaskRequest();
        request.setTitle("New Task");

        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(columnRepository.findByIdAndTenantId(COLUMN_ID, TENANT_ID)).thenReturn(Optional.of(testColumn));
        when(mapper.toEntity(any(KanbanTaskRequest.class), eq(testColumn), any(), any())).thenReturn(testTask);
        when(taskRepository.save(testTask)).thenReturn(testTask);
        when(mapper.toResponse(testTask)).thenReturn(new KanbanTaskResponse());

        // Act
        KanbanTaskResponse result = service.createTask(COLUMN_ID, request);

        // Assert
        assertNotNull(result);
        assertEquals(testColumn, testTask.getColumn());
        verify(taskRepository).save(testTask);
    }

    @Test
    @DisplayName("Should move task to another column")
    void moveTask_ShouldUpdateColumn() {
        // Arrange
        KanbanColumn targetColumn = new KanbanColumn();
        targetColumn.setId(2L);
        targetColumn.setName("Doing");

        KanbanBoard targetBoard = new KanbanBoard();
        targetBoard.setId(1L);
        targetBoard.setTenantId(TENANT_ID);
        targetColumn.setBoard(targetBoard);

        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(testTask));
        when(columnRepository.findByIdAndTenantId(2L, TENANT_ID)).thenReturn(Optional.of(targetColumn));
        when(taskRepository.save(testTask)).thenReturn(testTask);
        when(mapper.toResponse(testTask)).thenReturn(new KanbanTaskResponse());

        // Act
        KanbanTaskResponse result = service.moveTask(TASK_ID, 2L);

        // Assert
        assertNotNull(result);
        assertEquals(targetColumn, testTask.getColumn());
        verify(taskRepository).save(testTask);
    }

    @Test
    @DisplayName("Should update task details")
    void updateTask_ShouldSaveUpdatedTask() {
        // Arrange
        KanbanTaskRequest request = new KanbanTaskRequest();
        request.setTitle("Updated Title");

        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(testTask));
        doNothing().when(mapper).updateTaskFromRequest(request, testTask);
        when(taskRepository.save(testTask)).thenReturn(testTask);
        when(mapper.toResponse(testTask)).thenReturn(new KanbanTaskResponse());

        // Act
        KanbanTaskResponse result = service.updateTask(TASK_ID, request);

        // Assert
        assertNotNull(result);
        verify(taskRepository).save(testTask);
    }

    @Test
    @DisplayName("Should delete task successfully")
    void deleteTask_ShouldRemoveTask() {
        // Arrange
        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(testTask));
        doNothing().when(taskRepository).deleteById(TASK_ID);

        // Act
        service.deleteTask(TASK_ID);

        // Assert
        verify(taskRepository).deleteById(TASK_ID);
    }

    @Test
    @DisplayName("Should update column title")
    void updateColumn_ShouldSaveUpdatedColumn() {
        // Arrange
        KanbanColumnRequest request = new KanbanColumnRequest();
        request.setName("Updated Column");

        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(columnRepository.findByIdAndTenantId(COLUMN_ID, TENANT_ID)).thenReturn(Optional.of(testColumn));
        doNothing().when(mapper).updateColumnFromRequest(request, testColumn);
        when(columnRepository.save(testColumn)).thenReturn(testColumn);
        when(mapper.toResponse(testColumn)).thenReturn(new KanbanColumnResponse());

        // Act
        KanbanColumnResponse result = service.updateColumn(COLUMN_ID, request);

        // Assert
        assertNotNull(result);
        verify(columnRepository).save(testColumn);
    }

    @Test
    @DisplayName("Should delete column and all its tasks")
    void deleteColumn_ShouldRemoveColumn() {
        // Arrange
        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        when(columnRepository.findByIdAndTenantId(COLUMN_ID, TENANT_ID)).thenReturn(Optional.of(testColumn));
        doNothing().when(columnRepository).delete(testColumn);

        // Act
        service.deleteColumn(COLUMN_ID);

        // Assert
        verify(columnRepository).delete(testColumn);
    }
}
