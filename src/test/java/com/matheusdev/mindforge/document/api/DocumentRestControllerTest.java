package com.matheusdev.mindforge.document.api;

import com.matheusdev.mindforge.document.dto.DocumentResponse;
import com.matheusdev.mindforge.document.mapper.DocumentMapper;
import com.matheusdev.mindforge.document.model.Document;
import com.matheusdev.mindforge.document.service.DocumentService;
import com.matheusdev.mindforge.document.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class DocumentRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private DocumentMapper documentMapper;

    private Document testDocument;
    private DocumentResponse testResponse;
    private static final Long DOC_ID = 1L;

    @BeforeEach
    void setUp() {
        testDocument = new Document();
        testDocument.setId(DOC_ID);
        testDocument.setFileName("test.txt");

        testResponse = new DocumentResponse();
        testResponse.setId(DOC_ID);
        testResponse.setFileName("test.txt");
    }

    @Test
    @DisplayName("GET /api/documents should return all documents")
    void getAllDocuments_ShouldReturnList() throws Exception {
        // Arrange
        when(documentService.findAll()).thenReturn(Arrays.asList(testDocument));
        when(documentMapper.toResponse(testDocument)).thenReturn(testResponse);

        // Act & Assert
        mockMvc.perform(get("/api/documents")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(DOC_ID));

        verify(documentService).findAll();
    }

    @Test
    @DisplayName("POST /api/documents/upload should upload file")
    void uploadFile_ShouldReturnCreatedDocument() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
        when(documentService.storeFile(any(), any(), any(), any(), any())).thenReturn(testDocument);
        when(documentMapper.toResponse(testDocument)).thenReturn(testResponse);

        // Act & Assert
        mockMvc.perform(multipart("/api/documents/upload")
                .file(file)
                .param("projectId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(DOC_ID));

        verify(documentService).storeFile(any(), eq(1L), any(), any(), any());
    }

    @Test
    @DisplayName("GET /api/documents/download/{fileName} should return file resource")
    void downloadFile_ShouldReturnResource() throws Exception {
        // Arrange
        String fileName = "test.txt";
        Resource resource = new ByteArrayResource("content".getBytes());
        // Since we cannot easily mock the servlet context mime type in this setup,
        // we'll just check if it returns OK and the resource.

        when(fileStorageService.loadFileAsResource(fileName)).thenReturn(resource);

        // Act & Assert
        mockMvc.perform(get("/api/documents/download/{fileName}", fileName))
                .andExpect(status().isOk())
                .andExpect(content().bytes("content".getBytes()));

        verify(fileStorageService).loadFileAsResource(fileName);
    }

    @Test
    @DisplayName("DELETE /api/documents/{id} should return no content")
    void deleteDocument_ShouldReturnNoContent() throws Exception {
        // Arrange
        doNothing().when(documentService).deleteDocument(DOC_ID);

        // Act & Assert
        mockMvc.perform(delete("/api/documents/{id}", DOC_ID))
                .andExpect(status().isNoContent());

        verify(documentService).deleteDocument(DOC_ID);
    }
}
