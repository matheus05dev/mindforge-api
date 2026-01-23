package com.matheusdev.mindforge.ai.service.chain;

import com.matheusdev.mindforge.ai.dto.PromptPair;
import com.matheusdev.mindforge.ai.memory.model.UserProfileAI;
import com.matheusdev.mindforge.ai.memory.service.MemoryService;
import com.matheusdev.mindforge.ai.service.DocumentAnalyzer;
import com.matheusdev.mindforge.ai.service.PromptBuilderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class PromptBuildingStep implements AIProcessingStep {

    private final PromptBuilderService promptBuilderService;
    private final MemoryService memoryService;
    private final DocumentAnalyzer documentAnalyzer;

    @Override
    public CompletableFuture<AIContext> execute(AIContext context) {
        return CompletableFuture.supplyAsync(() -> {
            log.info(">> [CHAIN] Step 3: Prompt Building");

            UserProfileAI userProfile = memoryService.getProfile(context.getUserId());
            String userPrompt = context.getRequest().prompt();

            // Build base prompt
            PromptPair userAwarePrompts = promptBuilderService.buildGenericPrompt(userPrompt, userProfile,
                    Optional.empty(), Optional.empty());
            String finalSystemPrompt = enrichSystemPromptWithGlossary(userAwarePrompts.systemPrompt(), userPrompt);

            // Add override if present
            if (context.getRequest().systemMessage() != null && !context.getRequest().systemMessage().isBlank()) {
                finalSystemPrompt += "\n\n" + context.getRequest().systemMessage();
            }

            return context.withFinalSystemPrompt(finalSystemPrompt);
        });
    }

    private String enrichSystemPromptWithGlossary(String base, String query) {
        Map<String, String> staticDefinitions = documentAnalyzer.getTermDefinitions();
        StringBuilder glossary = new StringBuilder();
        for (Map.Entry<String, String> entry : staticDefinitions.entrySet()) {
            if (query.toLowerCase().contains(entry.getKey())) {
                glossary.append("- ").append(entry.getValue()).append("\n");
            }
        }
        if (glossary.length() > 0) {
            return base + "\n\n### GLOSSÁRIO:\n" + glossary.toString();
        }
        return base;
    }
}
