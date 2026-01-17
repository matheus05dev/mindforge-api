package com.matheusdev.mindforge.ai.service;

import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.core.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PromptCacheService {

    /**
     * Executa a requisição para a IA com suporte a Cache (Idempotência).
     * Se uma requisição idêntica já foi feita recentemente, retorna o resultado do cache,
     * economizando tokens e tempo de processamento.
     *
     * @deprecated Este método está obsoleto e não funciona com a nova arquitetura de agentes.
     *             Use o AIContextService diretamente.
     */
    @Deprecated
    @Cacheable(value = CacheConfig.AI_PROMPTS_CACHE, key = "#request.toString()")
    public AIProviderResponse executeRequest(AIProviderRequest request) {
        log.warn("O método PromptCacheService.executeRequest está obsoleto e não deve ser usado.");
        // Lança uma exceção para indicar que este caminho não é mais válido.
        throw new UnsupportedOperationException("PromptCacheService não é mais compatível com a arquitetura de agentes. " +
                "Refatore a chamada para usar AIContextService diretamente.");
    }
}
