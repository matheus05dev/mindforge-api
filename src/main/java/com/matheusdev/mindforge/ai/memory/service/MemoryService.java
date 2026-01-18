package com.matheusdev.mindforge.ai.memory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusdev.mindforge.ai.dto.ChatRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.ai.memory.model.UserProfileAI;
import com.matheusdev.mindforge.ai.memory.repository.UserProfileAIRepository;
import com.matheusdev.mindforge.ai.service.AIOrchestrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class MemoryService {

    private final UserProfileAIRepository userProfileAIRepository;
    private final AIOrchestrationService aiOrchestrationService;
    private final ObjectMapper objectMapper;

    // Construtor manual para quebrar a referência circular com @Lazy
    public MemoryService(UserProfileAIRepository userProfileAIRepository, @Lazy AIOrchestrationService aiOrchestrationService, ObjectMapper objectMapper) {
        this.userProfileAIRepository = userProfileAIRepository;
        this.aiOrchestrationService = aiOrchestrationService;
        this.objectMapper = objectMapper;
    }

    public UserProfileAI getProfile(Long userId) {
        return userProfileAIRepository.findById(userId)
                .orElseGet(() -> createDefaultProfile(userId));
    }

    private UserProfileAI createDefaultProfile(Long userId) {
        log.info("Criando perfil padrão de IA para o usuário: {}", userId);
        UserProfileAI newProfile = new UserProfileAI();
        newProfile.setId(userId);
        newProfile.setSummary("Novo usuário. Perfil ainda não analisado.");
        newProfile.setStructuredProfile("{}");
        return userProfileAIRepository.save(newProfile);
    }

    @Async
    @Transactional
    public void updateUserProfile(Long userId, List<Map<String, String>> chatHistory) {
        log.info("Iniciando atualização assíncrona do perfil de IA para o usuário: {}", userId);

        String metaPrompt = buildProfileUpdatePrompt(chatHistory);

        if (metaPrompt == null) {
            log.info("Nenhuma interação válida para atualizar o perfil do usuário {}. A atualização foi abortada.", userId);
            return;
        }

        try {
            String defaultProvider = "ollamaProvider"; // Ollama é bom para extração de JSON
            ChatRequest chatRequest = new ChatRequest(metaPrompt, defaultProvider, null, null);
            AIProviderResponse response = aiOrchestrationService.handleChatInteraction(chatRequest).get();

            if (response.getError() != null || response.getContent() == null) {
                log.error("Falha ao atualizar perfil de IA para o usuário {}: {}", userId, response.getError());
                return;
            }

            String jsonResponse = response.getContent();
            Map<String, Object> structuredProfile = objectMapper.readValue(jsonResponse, Map.class);
            String summary = (String) structuredProfile.getOrDefault("summary", "N/A");

            UserProfileAI profile = userProfileAIRepository.findById(userId).orElse(new UserProfileAI());
            profile.setId(userId);
            profile.setSummary(summary);
            profile.setStructuredProfile(jsonResponse);

            userProfileAIRepository.save(profile);
            log.info("Perfil de IA para o usuário {} atualizado com sucesso.", userId);

        } catch (InterruptedException | ExecutionException | IOException e) {
            log.error("Erro ao processar a atualização do perfil de IA para o usuário {}: {}", userId, e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private String buildProfileUpdatePrompt(List<Map<String, String>> chatHistory) {
        StringBuilder historyStr = new StringBuilder();
        List<String> systemErrorKeywords = List.of(
                "429 Too Many Requests",
                "CircuitBreaker",
                "instabilidade no serviço",
                "indisponível ou falhou"
        );
        List<String> assistantDenialKeywords = List.of(
                "não encontrei",
                "não há menção",
                "não foi possível fornecer",
                "não há menção explícita",
                "não pôde ser obtido"
        );

        for (Map<String, String> message : chatHistory) {
            String role = message.get("role");
            String content = message.get("content");

            // Ignora mensagens de erro do sistema
            boolean isSystemError = systemErrorKeywords.stream().anyMatch(content::contains);
            if (isSystemError) {
                log.debug("Mensagem de erro de sistema ignorada na atualização do perfil.");
                continue;
            }

            // Ignora mensagens de negação do assistente (indicativo de falha no RAG)
            if ("assistant".equals(role)) {
                String lowerCaseContent = content.toLowerCase();
                boolean isDenial = assistantDenialKeywords.stream().anyMatch(lowerCaseContent::contains);
                if (isDenial) {
                    log.debug("Mensagem de negação do assistente ignorada para proteger o perfil do usuário.");
                    continue;
                }
            }

            historyStr.append(String.format("[%s]: %s\n", role, content));
        }

        if (historyStr.length() == 0) {
            return null;
        }

        return String.format(
            "Sua tarefa é atuar como um psicólogo de aprendizado. Analise a conversa abaixo entre um 'assistant' (você) e um 'user'.\n" +
            "Com base na conversa, extraia um perfil de aprendizado do usuário. Identifique seus pontos fortes, fracos e os últimos tópicos que ele demonstrou interesse ou dificuldade.\n\n" +
            "**REGRAS IMPORTANTES:**\n" +
            "1. Sua resposta deve ser **APENAS** um objeto JSON válido, sem nenhum texto adicional antes ou depois.\n" +
            "2. O JSON deve ter a seguinte estrutura: `{\"summary\": \"...\", \"strengths\": [\"...\"], \"weaknesses\": [\"...\"]}`.\n" +
            "3. O campo 'summary' deve ser um resumo em uma frase sobre o perfil do usuário.\n\n" +
            "--- HISTÓRICO DA CONVERSA PARA ANÁLISE ---\n" +
            "%s",
            historyStr.toString()
        );
    }
}
