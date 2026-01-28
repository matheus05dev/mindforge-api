package com.matheusdev.mindforge.document.service;

import com.matheusdev.mindforge.document.model.Document;
import com.matheusdev.mindforge.document.repository.DocumentRepository;
import com.matheusdev.mindforge.exception.ResourceNotFoundException;
import com.matheusdev.mindforge.project.model.Project;
import com.matheusdev.mindforge.project.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private DocumentService service;

    private Document testDocument;
    private Project testProject;
    private static final Long DOC_ID = 1L;
    private static final Long PROJECT_ID = 1L;

    @BeforeEach
    void setUp() {
        testProject = new Project();
        testProject.setId(PROJECT_ID);
        testProject.setName("Test Project");

        testDocument = new Document();
        testDocument.setId(DOC_ID);
        testDocument.setFileName("test_file.txt");
        testDocument.setOriginalFileName("original.txt");
        testDocument.setProject(testProject);
    }

    @Test
    @DisplayName("Should store file and link to project")
    void storeFile_ShouldSaveDocument_WhenProjectExists() throws IOException {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "original.txt", "text/plain", "Hello World".getBytes());
        String storedFileName = "uuid_original.txt";

        when(fileStorageService.storeFile(any(MultipartFile.class))).thenReturn(storedFileName);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(testProject));
        when(documentRepository.save(any(Document.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Document result = service.storeFile(file, PROJECT_ID, null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(storedFileName, result.getFileName());
        assertEquals("original.txt", result.getOriginalFileName());
        assertEquals(testProject, result.getProject());
        verify(documentRepository).save(any(Document.class));
    }

    @Test
    @DisplayName("Should throw exception when project not found during storeFile")
    void storeFile_ShouldThrowException_WhenProjectNotFound() throws IOException {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "original.txt", "text/plain", "Hello".getBytes());
        when(fileStorageService.storeFile(any())).thenReturn("stored.txt");
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> service.storeFile(file, PROJECT_ID, null, null, null));
    }

    @Test
    @DisplayName("Should return all documents")
    void findAll_ShouldReturnList() {
        // Arrange
        when(documentRepository.findAll()).thenReturn(Arrays.asList(testDocument));

        // Act
        List<Document> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(documentRepository).findAll();
    }

    @Test
    @DisplayName("Should return document by ID")
    void findDocumentById_ShouldReturnDocument() {
        // Arrange
        when(documentRepository.findById(DOC_ID)).thenReturn(Optional.of(testDocument));

        // Act
        Document result = service.findDocumentById(DOC_ID);

        // Assert
        assertNotNull(result);
        assertEquals(DOC_ID, result.getId());
    }

    @Test
    @DisplayName("Should get document content as bytes")
    void getDocumentContent_ShouldReturnBytes() throws IOException {
        // Arrange
        byte[] content = "File content".getBytes();
        when(fileStorageService.loadFileAsBytes(testDocument.getFileName())).thenReturn(content);

        // Act
        byte[] result = service.getDocumentContent(testDocument);

        // Assert
        assertArrayEquals(content, result);
        verify(fileStorageService).loadFileAsBytes(testDocument.getFileName());
    }

    @Test
    @DisplayName("Should delete document and physical file")
    void deleteDocument_ShouldRemoveFileAndRecord() {
        // Arrange
        when(documentRepository.findById(DOC_ID)).thenReturn(Optional.of(testDocument));
        doNothing().when(fileStorageService).deleteFile(testDocument.getFileName());
        doNothing().when(documentRepository).delete(testDocument);

        // Act
        service.deleteDocument(DOC_ID);

        // Assert
        verify(fileStorageService).deleteFile(testDocument.getFileName());
        verify(documentRepository).delete(testDocument);
    }
}
