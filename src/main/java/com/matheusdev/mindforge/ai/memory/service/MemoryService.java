package com.matheusdev.mindforge.ai.memory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    public MemoryService(UserProfileAIRepository userProfileAIRepository,
            @Lazy AIOrchestrationService aiOrchestrationService, ObjectMapper objectMapper) {
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
        try {
            long delay = 5000L; // 5 segundos
            log.debug("Aguardando {}ms antes de iniciar análise de perfil para aliviar carga...", delay);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Delay de perfil interrompido.");
            return;
        }

        log.info("Iniciando atualização assíncrona do perfil de IA para o usuário: {}", userId);

        String metaPrompt = buildProfileUpdatePrompt(chatHistory);

        if (metaPrompt == null) {
            log.info("Nenhuma interação válida para atualizar o perfil do usuário {}. A atualização foi abortada.",
                    userId);
            return;
        }

        try {
            String systemMessage = "Sua tarefa é atuar como um psicólogo de aprendizado. Analise a conversa e retorne APENAS um objeto JSON válido.";
            // Agora usa o DEFAULT_PROVIDER (Ollama) conforme alterado no
            // AIOrchestrationService
            AIProviderResponse response = aiOrchestrationService.executeInternalAnalysis(metaPrompt, systemMessage)
                    .get();

            if (response.getError() != null || response.getContent() == null) {
                log.error("Falha ao atualizar perfil de IA para o usuário {}: {}", userId, response.getError());
                return;
            }

            String cleanedJsonResponse = cleanAIResponse(response.getContent());
            Map<String, Object> structuredProfile = objectMapper.readValue(cleanedJsonResponse, Map.class);
            String summary = (String) structuredProfile.getOrDefault("summary", "N/A");

            UserProfileAI profile = userProfileAIRepository.findById(userId).orElse(new UserProfileAI());
            profile.setId(userId);
            profile.setSummary(summary);
            profile.setStructuredProfile(cleanedJsonResponse);

            userProfileAIRepository.save(profile);
            log.info("Perfil de IA para o usuário {} atualizado com sucesso.", userId);

        } catch (InterruptedException | ExecutionException | IOException e) {
            log.error("Erro ao processar a atualização do perfil de IA para o usuário {}: {}", userId, e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private String cleanAIResponse(String rawContent) {
        if (rawContent == null || rawContent.isBlank())
            return "{}";

        String cleaned = rawContent.trim();

        // 1. Tenta extrair bloco de código JSON markdown ```json ... ```
        if (cleaned.contains("```")) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?s)```(?:json)?\\s*(\\{.*?\\})\\s*```");
            java.util.regex.Matcher matcher = pattern.matcher(cleaned);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        // 2. Se não achar bloco markdown (ou se estiver mal formatado), tenta achar o
        // primeiro objeto JSON {...}
        int firstBrace = cleaned.indexOf("{");
        int lastBrace = cleaned.lastIndexOf("}");

        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return cleaned.substring(firstBrace, lastBrace + 1);
        }

        // 3. Retorno original (fallback, provavelmente vai falhar se não for JSON puro)
        return cleaned;
    }

    private String buildProfileUpdatePrompt(List<Map<String, String>> chatHistory) {
        StringBuilder historyStr = new StringBuilder();
        List<String> systemErrorKeywords = List.of(
                "429 Too Many Requests",
                "CircuitBreaker",
                "instabilidade no serviço",
                "indisponível ou falhou");
        List<String> assistantDenialKeywords = List.of(
                "não encontrei",
                "não há menção",
                "não foi possível fornecer",
                "não há menção explícita",
                "não encontrada",
                "não foi encontrada",
                "sem informações nas evidências",
                "informação não encontrada",
                "não há menção",
                "não foi possível fornecer",
                "não há menção explícita",
                "não pôde ser obtido");

        for (Map<String, String> message : chatHistory) {
            String role = message.get("role");
            String content = message.get("content");

            boolean isSystemError = systemErrorKeywords.stream().anyMatch(content::contains);
            if (isSystemError) {
                log.debug("Mensagem de erro de sistema ignorada na atualização do perfil.");
                continue;
            }

            if ("assistant".equals(role)) {
                String lowerCaseContent = content.toLowerCase();
                boolean isDenial = assistantDenialKeywords.stream().anyMatch(lowerCaseContent::contains);
                if (isDenial) {
                    log.debug("Mensagem de negação do assistente ignorada para proteger o perfil do usuário.");
                    continue;
                }
            }

            String processedContent = extractContentForAnalysis(content);
            historyStr.append(String.format("[%s]: %s\n", role, processedContent));
        }

        if (historyStr.length() == 0) {
            return null;
        }

        return String.format(
                "Analise a conversa abaixo entre um 'assistant' e um 'user'.\n" +
                        "Extraia um perfil de aprendizado do usuário, identificando pontos fortes, fracos e tópicos de interesse ou dificuldade.\n"
                        +
                        "IMPORTANTE: Ignore falhas do assistente em encontrar informações. Foque na qualidade das perguntas do usuário.\n\n"
                        +
                        "**REGRAS IMPORTANTES:**\n" +
                        "1. Sua resposta deve ser **APENAS** um objeto JSON válido.\n" +
                        "2. O JSON deve ter a seguinte estrutura: `{\"summary\": \"...\", \"strengths\": [\"...\"], \"weaknesses\": [\"...\"]}`.\n"
                        +
                        "3. O campo 'summary' deve ser um resumo em uma frase sobre o perfil do usuário.\n\n" +
                        "--- HISTÓRICO DA CONVERSA PARA ANÁLISE ---\n" +
                        "%s",
                historyStr.toString());
    }

    private String extractContentForAnalysis(String rawContent) {
        if (rawContent == null)
            return "";

        try {
            if (rawContent.trim().startsWith("{")) {
                Map<String, Object> json = objectMapper.readValue(rawContent, Map.class);
                if (json.containsKey("answer")) {
                    Object answerObj = json.get("answer");
                    if (answerObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> answerMap = (Map<String, Object>) answerObj;
                        return (String) answerMap.getOrDefault("plainText",
                                answerMap.getOrDefault("markdown", rawContent));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Falha ao fazer parse do conteúdo para análise de memória (usando raw): {}", e.getMessage());
        }

        // Truncate to avoid exploding tokens (e.g. 1000 chars max per message)
        if (rawContent.length() > 1000) {
            return rawContent.substring(0, 1000) + "... [truncado]";
        }
        return rawContent;

    }
}
