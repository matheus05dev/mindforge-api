package com.matheusdev.mindforge.ai.service;

import com.matheusdev.mindforge.ai.provider.AIProvider;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service("AIOrchestratorService")
@Primary
public class AIOrchestratorService implements AIProvider {

    private final AIProvider geminiProvider;
    private final AIProvider groqProvider;

    public AIOrchestratorService(
            @Qualifier("geminiProvider") AIProvider geminiProvider,
            @Qualifier("groqProvider") AIProvider groqProvider
    ) {
        this.geminiProvider = geminiProvider;
        this.groqProvider = groqProvider;
    }

    @Override
    public CompletableFuture<AIProviderResponse> executeTask(AIProviderRequest request) {
        String preferredProvider = request.preferredProvider();

        // Roteamento baseado na escolha do usuário
        if (preferredProvider != null) {
            switch (preferredProvider.toLowerCase()) {
                case "gemini":
                    return geminiProvider.executeTask(request);
                case "groq":
                    return groqProvider.executeTask(request);
            }
        }

        // Lógica padrão "Mindforge AI": escolher o melhor provedor
        return chooseBestProvider(request);
    }

    /**
     * Lógica do "Mindforge AI": analisa a requisição e escolhe o provedor mais adequado.
     */
    private CompletableFuture<AIProviderResponse> chooseBestProvider(AIProviderRequest request) {
        // Exemplo de heurística: se o prompt for muito longo, use um modelo com maior capacidade de contexto.
        // Esta lógica pode ser tão complexa quanto necessário.
        if (request.textPrompt().length() > 4000) {
            return geminiProvider.executeTask(request); // Supondo que Gemini tenha um contexto maior
        }
        
        // Para outras tarefas, podemos usar o Groq por sua velocidade.
        return groqProvider.executeTask(request);
    }
}
