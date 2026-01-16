package com.matheusdev.mindforge.ai.memory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusdev.mindforge.ai.provider.AIProvider;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.ai.memory.model.UserProfileAI;
import com.matheusdev.mindforge.ai.memory.repository.UserProfileAIRepository;
import com.matheusdev.mindforge.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class MemoryService {

    private final UserProfileAIRepository userProfileAIRepository;
    private final AIProvider aiProvider;
    private final ObjectMapper objectMapper;

    public MemoryService(UserProfileAIRepository userProfileAIRepository,
                         @Qualifier("AIOrchestratorService") AIProvider aiProvider,
                         ObjectMapper objectMapper) {
        this.userProfileAIRepository = userProfileAIRepository;
        this.aiProvider = aiProvider;
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
        
        AIProviderRequest request = new AIProviderRequest(metaPrompt);
        // Usando .join() para esperar o CompletableFuture
        AIProviderResponse response = aiProvider.executeTask(request).join();

        if (response.getError() != null || response.getContent() == null) {
            log.error("Falha ao atualizar perfil de IA para o usuário {}: {}", userId, response.getError());
            return;
        }

        try {
            String jsonResponse = response.getContent();
            Map<String, Object> structuredProfile = objectMapper.readValue(jsonResponse, Map.class);
            String summary = (String) structuredProfile.getOrDefault("summary", "N/A");

            UserProfileAI profile = userProfileAIRepository.findById(userId).orElse(new UserProfileAI());
            profile.setId(userId);
            profile.setSummary(summary);
            profile.setStructuredProfile(jsonResponse);

            userProfileAIRepository.save(profile);
            log.info("Perfil de IA para o usuário {} atualizado com sucesso.", userId);

        } catch (Exception e) {
            log.error("Erro ao processar a resposta JSON da IA para o perfil do usuário {}: {}", userId, e.getMessage());
        }
    }

    private String buildProfileUpdatePrompt(List<Map<String, String>> chatHistory) {
        StringBuilder historyStr = new StringBuilder();
        for (Map<String, String> message : chatHistory) {
            historyStr.append(String.format("[%s]: %s\n", message.get("role"), message.get("content")));
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
