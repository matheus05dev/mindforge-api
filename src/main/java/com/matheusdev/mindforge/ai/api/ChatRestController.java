package com.matheusdev.mindforge.ai.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusdev.mindforge.ai.chat.model.ChatSession;
import com.matheusdev.mindforge.ai.dto.ChatRequest;
import com.matheusdev.mindforge.ai.dto.ChatResponseDTO;
import com.matheusdev.mindforge.ai.service.AIOrchestrationService;
import com.matheusdev.mindforge.ai.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/v1/ai/chat")
@RequiredArgsConstructor
@Tag(name = "AI Chat", description = "Endpoints para interações diretas de chat com modelos de IA")
public class ChatRestController {

    private final AIOrchestrationService aiOrchestrationService;
    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    @PostMapping
    @Operation(summary = "Envia um prompt para o provedor de IA selecionado", description = "Permite enviar um prompt de texto direto para um provedor de IA (ex: Ollama, Groq) e receber uma resposta. Você pode especificar o provedor, o modelo e uma mensagem de sistema.")
    public ResponseEntity<?> chat(@RequestBody ChatRequest chatRequest) {
        try {
            return aiOrchestrationService.handleChatInteraction(chatRequest)
                    .thenApply(response -> {
                        String content = response.getContent();
                        System.out.println(">>> [CHAT] Resposta recebida: " + content);
                        try {
                            // Tenta desserializar a String JSON para um objeto real
                            Object auditedObj = objectMapper.readValue(content, Object.class);

                            // INJECT SESSION ID: O frontend precisa do ID para continuar a conversa
                            if (auditedObj instanceof java.util.Map) {
                                try {
                                    ((java.util.Map<String, Object>) auditedObj).put("sessionId",
                                            response.getSessionId());
                                } catch (Exception e) {
                                    // Ignore se for mapa imutável (embora jackson retorne LinkedHashMap mutável)
                                }
                            }

                            // Retorna o objeto auditado diretamente como corpo da resposta
                            return ResponseEntity.ok(auditedObj);
                        } catch (IOException e) {
                            System.out.println(
                                    ">>> [CHAT] Falha ao parsear JSON, retornando como texto: " + e.getMessage());
                            // Se falhou, retorna a estrutura antiga com ChatResponseDTO para texto/erro
                            if (content.startsWith("❌")) {
                                return ResponseEntity.ok(new ChatResponseDTO(content, "ERROR"));
                            } else {
                                return ResponseEntity.ok(new ChatResponseDTO(content, "TEXT"));
                            }
                        }
                    })
                    .exceptionally(ex -> {
                        System.err.println(">>> [CHAT] ERRO no processamento: " + ex.getMessage());
                        ex.printStackTrace();
                        return ResponseEntity
                                .ok(new ChatResponseDTO("Erro ao processar mensagem: " + ex.getMessage(), "ERROR"));
                    })
                    .get(); // WAIT for completion before returning
        } catch (Exception e) {
            System.err.println(">>> [CHAT] ERRO CRÍTICO: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(new ChatResponseDTO("Erro crítico: " + e.getMessage(), "ERROR"));
        }
    }

    @PostMapping("/session")
    @Operation(summary = "Cria uma nova sessão de chat", description = "Cria uma nova sessão de chat vazia. O corpo da requisição é opcional.")
    public ResponseEntity<com.matheusdev.mindforge.knowledgeltem.dto.ChatSessionResponse> createSession(
            @RequestBody(required = false) Map<String, Object> request) {
        // Se o front enviar um JSON vazio ou nulo, criamos uma sessão padrão
        ChatSession session = chatService.createNewSession();
        return ResponseEntity.ok(chatService.mapToResponse(session));
    }

    @org.springframework.web.bind.annotation.GetMapping
    @Operation(summary = "Lista todas as sessões de chat", description = "Retorna o histórico de todas as sessões de chat, ordenadas da mais recente para a mais antiga.")
    public ResponseEntity<java.util.List<com.matheusdev.mindforge.knowledgeltem.dto.ChatSessionResponse>> getAllSessions() {
        return ResponseEntity.ok(chatService.getAllSessions().stream()
                .map(chatService::mapToResponse)
                .collect(java.util.stream.Collectors.toList()));
    }

    @org.springframework.web.bind.annotation.GetMapping("/{id}")
    @Operation(summary = "Obtém uma sessão de chat específica", description = "Retorna os detalhes e mensagens de uma sessão pelo ID.")
    public ResponseEntity<com.matheusdev.mindforge.knowledgeltem.dto.ChatSessionResponse> getSession(
            @org.springframework.web.bind.annotation.PathVariable Long id) {
        return chatService.getSession(id)
                .map(session -> ResponseEntity.ok(chatService.mapToResponse(session)))
                .orElse(ResponseEntity.notFound().build());
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}")
    @Operation(summary = "Atualiza o título de uma sessão de chat", description = "Permite renomear uma sessão de chat existente.")
    public ResponseEntity<com.matheusdev.mindforge.knowledgeltem.dto.ChatSessionResponse> updateSessionTitle(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String newTitle = request.get("title");
        if (newTitle == null || newTitle.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        return chatService.updateSessionTitle(id, newTitle)
                .map(session -> ResponseEntity.ok(chatService.mapToResponse(session)))
                .orElse(ResponseEntity.notFound().build());
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    @Operation(summary = "Exclui uma sessão de chat", description = "Remove permanentemente uma sessão de chat e suas mensagens.")
    public ResponseEntity<Void> deleteSession(@org.springframework.web.bind.annotation.PathVariable Long id) {
        if (chatService.deleteSession(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

}
