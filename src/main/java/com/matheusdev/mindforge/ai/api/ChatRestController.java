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
import java.util.concurrent.CompletableFuture;

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
    public CompletableFuture<ResponseEntity<?>> chat(@RequestBody ChatRequest chatRequest) {
        return aiOrchestrationService.handleChatInteraction(chatRequest)
                .thenApply(response -> {
                    String content = response.getContent();
                    try {
                        // Tenta desserializar a String JSON para um objeto real
                        Object auditedObj = objectMapper.readValue(content, Object.class);
                        // Retorna o objeto auditado diretamente como corpo da resposta
                        return ResponseEntity.ok(auditedObj);
                    } catch (IOException e) {
                        // Se falhou, retorna a estrutura antiga com ChatResponseDTO para texto/erro
                        if (content.startsWith("❌")) {
                            return ResponseEntity.ok(new ChatResponseDTO(content, "ERROR"));
                        } else {
                            return ResponseEntity.ok(new ChatResponseDTO(content, "TEXT"));
                        }
                    }
                });
    }

    @PostMapping("/session")
    @Operation(summary = "Cria uma nova sessão de chat", description = "Cria uma nova sessão de chat vazia. O corpo da requisição é opcional.")
    public ResponseEntity<ChatSession> createSession(@RequestBody(required = false) Map<String, Object> request) {
        // Se o front enviar um JSON vazio ou nulo, criamos uma sessão padrão
        return ResponseEntity.ok(chatService.createEmergencySession());
    }

    @org.springframework.web.bind.annotation.GetMapping
    @Operation(summary = "Lista todas as sessões de chat", description = "Retorna o histórico de todas as sessões de chat, ordenadas da mais recente para a mais antiga.")
    public ResponseEntity<java.util.List<ChatSession>> getAllSessions() {
        return ResponseEntity.ok(chatService.getAllSessions());
    }

    @org.springframework.web.bind.annotation.GetMapping("/{id}")
    @Operation(summary = "Obtém uma sessão de chat específica", description = "Retorna os detalhes e mensagens de uma sessão pelo ID.")
    public ResponseEntity<ChatSession> getSession(@org.springframework.web.bind.annotation.PathVariable Long id) {
        return chatService.getSession(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
