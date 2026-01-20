package com.matheusdev.mindforge.ai.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusdev.mindforge.ai.dto.ChatResponseDTO;
import com.matheusdev.mindforge.ai.service.AIOrchestrationService;
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
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/v1/ai/document")
@RequiredArgsConstructor
@Tag(name = "Processamento de Documentos com IA", description = "Endpoints para interagir com a IA enviando documentos")
@Slf4j
public class DocumentAIRestController {

    private final AIOrchestrationService aiOrchestrationService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Analisa um documento com um prompt de texto", description = "Faça upload de um documento (ex: PDF, DOCX, TXT) e forneça um prompt de texto. O serviço extrai o texto do documento, combina com seu prompt e envia para o provedor de IA selecionado para análise.")
    public ResponseEntity<?> analyzeDocument(
            @Parameter(description = "O arquivo do documento a ser analisado.", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "O prompt de texto para guiar a análise da IA.", required = true) @RequestParam("prompt") String prompt,
            @Parameter(description = "O provedor de IA a ser usado. Opções: 'ollamaProvider', 'groqProvider'. Padrão: 'ollamaProvider'.") @RequestParam(value = "provider", required = false) String provider)
            throws IOException {

        log.info(">>> DocumentAIRestController: Recebida requisição de análise de documento.");
        log.info("Nome do arquivo: {}, Tamanho: {} bytes, Prompt: '{}', Provedor: '{}'",
                file.getOriginalFilename(), file.getSize(), prompt, provider);

        try {
            // Wait for the async operation to complete and get the result
            var response = aiOrchestrationService.handleFileAnalysis(prompt, provider, file).join();
            
            String content = response.getContent();
            try {
                // Tenta desserializar a String JSON para um Map para injetar o sessionId
                Map<String, Object> auditedObj = objectMapper.readValue(content,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                        });

                // Injeta o ID da sessão para o Frontend atualizar o contexto
                if (response.getSessionId() != null) {
                    auditedObj.put("sessionId", response.getSessionId());
                }

                // Retorna o objeto auditado (com sessionId) diretamente
                return ResponseEntity.ok(auditedObj);
            } catch (IOException e) {
                // Se falhou, retorna a estrutura antiga com ChatResponseDTO para texto/erro
                log.warn(
                        "Não foi possível parsear a resposta como JSON auditado. Tratando como texto. Conteúdo: {}",
                        content);
                if (content.startsWith("❌")) {
                    return ResponseEntity.ok(new ChatResponseDTO(content, "ERROR"));
                } else {
                    return ResponseEntity.ok(new ChatResponseDTO(content, "TEXT"));
                }
            }
        } catch (Exception e) {
            log.error("Erro ao processar análise de documento", e);
            return ResponseEntity.ok(new ChatResponseDTO("❌ Erro ao processar documento: " + e.getMessage(), "ERROR"));
        }
    }
}
