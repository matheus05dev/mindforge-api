package com.matheusdev.mindforge.study.roadmap.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusdev.mindforge.ai.service.AIOrchestrationService;
import com.matheusdev.mindforge.core.auth.util.SecurityUtils;
import com.matheusdev.mindforge.study.roadmap.dto.RoadmapDTOs;
import com.matheusdev.mindforge.study.roadmap.model.Roadmap;
import com.matheusdev.mindforge.study.roadmap.model.RoadmapItem;
import com.matheusdev.mindforge.study.roadmap.repository.RoadmapRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoadmapServiceTest {

    @Mock
    private RoadmapRepository roadmapRepository;

    @Mock
    private AIOrchestrationService aiOrchestrationService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RoadmapService roadmapService;

    private MockedStatic<SecurityUtils> securityUtilsMock;
    private Roadmap testRoadmap;
    private RoadmapDTOs.RoadmapResponse testRoadmapResponse;
    private static final Long USER_ID = 1L;
    private static final Long TENANT_ID = 1L;
    private static final Long ROADMAP_ID = 1L;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);

        testRoadmap = Roadmap.builder()
                .id(ROADMAP_ID)
                .title("Java Learning Path")
                .description("Complete Java roadmap")
                .targetAudience("Beginners")
                .userId(USER_ID)
                .tenantId(TENANT_ID)
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();

        RoadmapItem item1 = RoadmapItem.builder()
                .id(1L)
                .roadmap(testRoadmap)
                .title("Week 1: Java Basics")
                .description("Learn Java fundamentals")
                .orderIndex(0)
                .resourcesJson("[]")
                .build();

        testRoadmap.getItems().add(item1);

        testRoadmapResponse = RoadmapDTOs.RoadmapResponse.builder()
                .id(ROADMAP_ID)
                .title("Java Learning Path")
                .description("Complete Java roadmap")
                .targetAudience("Beginners")
                .createdAt(LocalDateTime.now().toString())
                .items(new ArrayList<>())
                .build();
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    @DisplayName("Should generate and save roadmap successfully")
    void generateAndSaveRoadmap_ShouldCreateRoadmap() throws JsonProcessingException {
        // Arrange
        String topic = "Java Programming";
        String duration = "3 months";
        String difficulty = "beginner";

        RoadmapDTOs.ResourceLink resource = RoadmapDTOs.ResourceLink.builder()
                .title("Java Tutorial")
                .url("https://example.com")
                .type("article")
                .build();

        RoadmapDTOs.RoadmapItemResponse itemResponse = RoadmapDTOs.RoadmapItemResponse.builder()
                .orderIndex(0)
                .title("Week 1: Java Basics")
                .description("Learn fundamentals")
                .resources(Arrays.asList(resource))
                .build();

        RoadmapDTOs.RoadmapResponse generatedResponse = RoadmapDTOs.RoadmapResponse.builder()
                .title("Java Learning Path")
                .description("Complete roadmap")
                .targetAudience("Beginners")
                .items(Arrays.asList(itemResponse))
                .build();

        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);
        when(aiOrchestrationService.generateRoadmap(topic, duration, difficulty))
                .thenReturn(CompletableFuture.completedFuture(generatedResponse));
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");
        when(roadmapRepository.save(any(Roadmap.class))).thenReturn(testRoadmap);
        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(Arrays.asList(resource));

        // Act
        RoadmapDTOs.RoadmapResponse result = roadmapService.generateAndSaveRoadmap(topic, duration, difficulty);

        // Assert
        assertNotNull(result);
        verify(aiOrchestrationService).generateRoadmap(topic, duration, difficulty);
        verify(roadmapRepository).save(any(Roadmap.class));
    }

    @Test
    @DisplayName("Should return user roadmaps")
    void getUserRoadmaps_ShouldReturnRoadmaps() throws JsonProcessingException {
        // Arrange
        securityUtilsMock.when(SecurityUtils::getCurrentTenantId).thenReturn(TENANT_ID);
        when(roadmapRepository.findByTenantIdOrderByCreatedAtDesc(TENANT_ID))
                .thenReturn(Arrays.asList(testRoadmap));
        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(new ArrayList<>());

        // Act
        List<RoadmapDTOs.RoadmapResponse> result = roadmapService.getUserRoadmaps();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Java Learning Path", result.get(0).getTitle());
        verify(roadmapRepository).findByTenantIdOrderByCreatedAtDesc(TENANT_ID);
    }

    @Test
    @DisplayName("Should return roadmap by ID")
    void getRoadmap_ShouldReturnRoadmap_WhenRoadmapExists() throws JsonProcessingException {
        // Arrange
        securityUtilsMock.when(SecurityUtils::getCurrentTenantId).thenReturn(TENANT_ID);
        when(roadmapRepository.findByIdAndTenantId(ROADMAP_ID, TENANT_ID))
                .thenReturn(Optional.of(testRoadmap));
        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(new ArrayList<>());

        // Act
        RoadmapDTOs.RoadmapResponse result = roadmapService.getRoadmap(ROADMAP_ID);

        // Assert
        assertNotNull(result);
        assertEquals("Java Learning Path", result.getTitle());
        verify(roadmapRepository).findByIdAndTenantId(ROADMAP_ID, TENANT_ID);
    }

    @Test
    @DisplayName("Should throw exception when roadmap not found")
    void getRoadmap_ShouldThrowException_WhenRoadmapNotFound() {
        // Arrange
        securityUtilsMock.when(SecurityUtils::getCurrentTenantId).thenReturn(TENANT_ID);
        when(roadmapRepository.findByIdAndTenantId(ROADMAP_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> roadmapService.getRoadmap(ROADMAP_ID));
        assertTrue(exception.getMessage().contains("Roadmap not found"));
        verify(roadmapRepository).findByIdAndTenantId(ROADMAP_ID, TENANT_ID);
    }
}
