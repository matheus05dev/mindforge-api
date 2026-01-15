package com.matheusdev.mindforge.ai.service;

import com.matheusdev.mindforge.ai.provider.AIProvider;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.core.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptCacheService {

    private final AIProvider aiProvider;

    /**
     * Executa a requisição para a IA com suporte a Cache (Idempotência).
     * Se uma requisição idêntica já foi feita recentemente, retorna o resultado do cache,
     * economizando tokens e tempo de processamento.
     */
    @Cacheable(value = CacheConfig.AI_PROMPTS_CACHE, key = "#request.toString()")
    public AIProviderResponse executeRequest(AIProviderRequest request) {
        log.info("Cache miss - Chamando provedor de IA para o prompt (hash): {}", request.hashCode());
        
        // Chama o provider (que tem Retry/CircuitBreaker) e aguarda o resultado para cachear o valor final
        return aiProvider.executeTask(request).join();
    }
}
