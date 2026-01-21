package com.matheusdev.mindforge.knowledgeltem.api;

import com.matheusdev.mindforge.ai.service.AIOrchestrationService;
import com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAIRequest;
import com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAIResponse;
import com.matheusdev.mindforge.knowledgeltem.dto.ChatSessionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/v1/knowledge/ai")
@RequiredArgsConstructor
@Tag(name = "Knowledge AI Assistant", description = "Endpoints para IA no Editor de Knowledge")
public class KnowledgeAIController {

    private final AIOrchestrationService aiOrchestrationService;
    private final com.matheusdev.mindforge.ai.chat.repository.ChatSessionRepository chatSessionRepository;
    private final com.matheusdev.mindforge.knowledgeltem.service.ProposalCacheService proposalCacheService;
    private final com.matheusdev.mindforge.knowledgeltem.service.KnowledgeBaseService knowledgeBaseService;

    @Operation(summary = "Obtém histórico de chat da nota", description = "Retorna a sessão de chat vinculada a um Knowledge Item.")
    @org.springframework.web.bind.annotation.GetMapping("/{knowledgeId}/session")
    public ResponseEntity<ChatSessionResponse> getChatHistory(
            @org.springframework.web.bind.annotation.PathVariable Long knowledgeId) {
        return chatSessionRepository.findByKnowledgeItemId(knowledgeId)
                .map(session -> {
                    ChatSessionResponse response = new ChatSessionResponse();
                    response.setId(session.getId());
                    response.setMessages(session.getMessages().stream().map(msg -> {
                        ChatSessionResponse.MessageResponse m = new ChatSessionResponse.MessageResponse();
                        m.setId(msg.getId());
                        m.setRole(msg.getRole().toString().toLowerCase()); // user/assistant
                        m.setContent(msg.getContent());
                        m.setCreatedAt(msg.getCreatedAt());
                        return m;
                    }).toList());
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Assistente de Escrita e Agente (Notion AI style)", description = "Processa comandos como continuar escrevendo, resumir, corrigir gramática ou perguntar ao agente.")
    @PostMapping("/assist")
    public ResponseEntity<KnowledgeAIResponse> assist(@RequestBody KnowledgeAIRequest request) {
        try {
            KnowledgeAIResponse response = aiOrchestrationService.processKnowledgeAssist(request).get();
            return ResponseEntity.ok(response);
        } catch (InterruptedException | ExecutionException e) {
            return ResponseEntity.internalServerError()
                    .body(new KnowledgeAIResponse(null, false, "Erro interno: " + e.getMessage()));
        }
    }

    @Operation(summary = "Aplica proposta aprovada", description = "Aplica as mudanças aprovadas de uma proposta do agente ao knowledge item.")
    @PostMapping("/proposals/{proposalId}/apply")
    public ResponseEntity<com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeItemResponse> applyProposal(
            @org.springframework.web.bind.annotation.PathVariable String proposalId,
            @RequestBody com.matheusdev.mindforge.knowledgeltem.dto.ApprovalRequest approval) {
        try {
            // 1. Get proposal from cache
            com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAgentProposal proposal = proposalCacheService
                    .getProposal(proposalId);

            if (proposal == null) {
                return ResponseEntity.notFound().build();
            }

            // 2. Apply changes
            com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeItemResponse updated = knowledgeBaseService
                    .applyProposal(proposal, approval);

            // 3. Remove proposal from cache
            proposalCacheService.removeProposal(proposalId);

            return ResponseEntity.ok(updated);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
