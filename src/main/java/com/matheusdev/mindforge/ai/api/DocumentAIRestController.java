package com.matheusdev.mindforge.ai.api;

import com.matheusdev.mindforge.ai.service.AIOrchestrationService;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/v1/ai/document")
@RequiredArgsConstructor
@Tag(name = "AI Document Processing", description = "Endpoints for interacting with AI by sending documents")
@Slf4j
public class DocumentAIRestController {

    private final AIOrchestrationService aiOrchestrationService;

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Analyze a document with a text prompt",
            description = "Upload a document (e.g., PDF, DOCX, TXT) and provide a text prompt. The service extracts text from the document, combines it with your prompt, and sends it to the selected AI provider for analysis.")
    public CompletableFuture<ResponseEntity<AIProviderResponse>> analyzeDocument(
            @Parameter(description = "The document file to be analyzed.", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "The text prompt to guide the AI's analysis.", required = true)
            @RequestParam("prompt") String prompt,
            @Parameter(description = "The AI provider to use. Options: 'geminiProvider', 'groqProvider'. Defaults to 'geminiProvider'.")
            @RequestParam(value = "provider", required = false) String provider) throws IOException {

        log.info(">>> DocumentAIRestController: Recebida requisição de análise de documento.");
        log.info("Nome do arquivo: {}, Tamanho: {} bytes, Prompt: '{}', Provedor: '{}'",
                file.getOriginalFilename(), file.getSize(), prompt, provider);

        return aiOrchestrationService.handleFileAnalysis(prompt, provider, file)
                .thenApply(ResponseEntity::ok);
    }
}
