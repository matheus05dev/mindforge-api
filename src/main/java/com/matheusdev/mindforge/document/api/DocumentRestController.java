package com.matheusdev.mindforge.document.api;

import com.matheusdev.mindforge.document.dto.DocumentResponse;
import com.matheusdev.mindforge.document.mapper.DocumentMapper;
import com.matheusdev.mindforge.document.model.Document;
import com.matheusdev.mindforge.document.service.DocumentService;
import com.matheusdev.mindforge.document.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Document management")
public class DocumentRestController {

    private final DocumentService documentService;
    private final FileStorageService fileStorageService;
    private final DocumentMapper documentMapper;

    @Operation(summary = "Get all documents", description = "Retrieves all uploaded documents")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved documents")
    })
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAllDocuments() {
        List<Document> documents = documentService.findAll();
        List<DocumentResponse> responses = documents.stream()
                .map(documentMapper::toResponse)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Get documents by project", description = "Retrieves documents associated with a specific project")
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<DocumentResponse>> getDocumentsByProject(@PathVariable Long projectId) {
        List<Document> documents = documentService.findByProjectId(projectId);
        List<DocumentResponse> responses = documents.stream()
                .map(documentMapper::toResponse)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Upload a document", description = "Uploads a document and associates it with a project, kanban task, knowledge item, or study session")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully uploaded the document")
    })
    @PostMapping("/upload")
    public ResponseEntity<DocumentResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long kanbanTaskId,
            @RequestParam(required = false) Long knowledgeItemId,
            @RequestParam(required = false) Long studySessionId) throws IOException {
        Document document = documentService.storeFile(file, projectId, kanbanTaskId, knowledgeItemId, studySessionId);
        return ResponseEntity.ok(documentMapper.toResponse(document));
    }

    @Operation(summary = "Download a document", description = "Downloads a document by its file name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully downloaded the document")
    })
    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
        Resource resource = fileStorageService.loadFileAsResource(fileName);

        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // ignore
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @Operation(summary = "View a document", description = "Views a document by its file name (inline)")
    @GetMapping("/view/{fileName:.+}")
    public ResponseEntity<Resource> viewFile(@PathVariable String fileName, HttpServletRequest request) {
        Resource resource = fileStorageService.loadFileAsResource(fileName);

        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // ignore
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @Operation(summary = "Delete a document", description = "Deletes a document by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully deleted the document")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }
}
