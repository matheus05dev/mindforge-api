package com.matheusdev.mindforge.ai.api;

import com.matheusdev.mindforge.ai.chat.model.ChatMessage;
import com.matheusdev.mindforge.ai.dto.*;
import com.matheusdev.mindforge.ai.service.AIService;
import com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Assistant", description = "Endpoints para interação com a IA")
public class AIRestController {

    private final AIService aiService;

    @Operation(summary = "Analisa código enviado diretamente", description = "Envia um trecho de código ou um documento para ser analisado pela IA em diferentes modos (Mentor, Analista, Debugger, Tutor Socrático).")
    @PostMapping("/analyze/code")
    public ResponseEntity<ChatMessage> analyzeCode(@RequestBody @Valid CodeAnalysisRequest request) {
        try {
            ChatMessage responseMessage = aiService.analyzeCodeForProficiency(request);
            return ResponseEntity.ok(responseMessage);
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }

    @Operation(summary = "Analisa arquivo do GitHub", description = "Envia um caminho de arquivo de um repositório do GitHub vinculado para ser analisado pela IA.")
    @PostMapping("/analyze/github-file")
    public ResponseEntity<ChatMessage> analyzeGitHubFile(@RequestBody @Valid GitHubFileAnalysisRequest request) {
        try {
            ChatMessage responseMessage = aiService.analyzeGitHubFile(request);
            return ResponseEntity.ok(responseMessage);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @Operation(summary = "Análise Genérica de Conhecimento", description = "Envia uma pergunta ou problema de qualquer área (matemática, gestão, etc.) para a IA resolver ou explicar.")
    @PostMapping("/analyze/generic")
    public ResponseEntity<ChatMessage> analyzeGeneric(@RequestBody @Valid GenericAnalysisRequest request) {
        ChatMessage responseMessage = aiService.analyzeGeneric(request);
        return ResponseEntity.ok(responseMessage);
    }

    @Operation(summary = "Modifica o conteúdo de uma anotação", description = "Pede à IA para reescrever, resumir, traduzir ou modificar o conteúdo de um KnowledgeItem existente.")
    @PostMapping("/edit/knowledge-item/{itemId}")
    public ResponseEntity<KnowledgeItemResponse> modifyKnowledgeItemContent(
            @PathVariable Long itemId,
            @RequestBody @Valid ContentModificationRequest request) {
        KnowledgeItemResponse updatedItem = aiService.modifyKnowledgeItemContent(itemId, request.getInstruction());
        return ResponseEntity.ok(updatedItem);
    }

    @Operation(summary = "Transcreve texto de uma imagem", description = "Usa OCR para extrair texto de um documento de imagem e o anexa a um KnowledgeItem.")
    @PostMapping("/transcribe/document/{documentId}/to-item/{itemId}")
    public ResponseEntity<KnowledgeItemResponse> transcribeImageToKnowledgeItem(
            @PathVariable Long documentId,
            @PathVariable Long itemId) {
        try {
            KnowledgeItemResponse updatedItem = aiService.transcribeImageAndAppendToKnowledgeItem(documentId, itemId);
            return ResponseEntity.ok(updatedItem);
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }

    @Operation(summary = "Revisa um portfólio do GitHub", description = "Atua como um Tech Recruiter para analisar um repositório completo (README, estrutura, código) e fornecer feedback de carreira.")
    @PostMapping("/review/portfolio")
    public ResponseEntity<ChatMessage> reviewPortfolio(@RequestBody @Valid PortfolioReviewRequest request) throws IOException {
        ChatMessage responseMessage = aiService.reviewPortfolio(request);
        return ResponseEntity.ok(responseMessage);
    }

    @Operation(summary = "Pensa como um Gerente de Produto", description = "Analisa uma ideia de funcionalidade e retorna uma análise de produto (User Story, UX, Trade-offs).")
    @PostMapping("/think/product")
    public ResponseEntity<ChatMessage> thinkAsProductManager(@RequestBody @Valid ProductThinkerRequest request) {
        ChatMessage responseMessage = aiService.thinkAsProductManager(request);
        return ResponseEntity.ok(responseMessage);
    }
}
