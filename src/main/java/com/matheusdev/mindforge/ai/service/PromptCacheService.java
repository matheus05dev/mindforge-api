package com.matheusdev.mindforge.ai.service;

import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.core.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class PromptCacheService {

    private final CacheManager cacheManager;

    /**
     * Executa a requisição para o provedor de IA gerenciando o cache manualmente.
     * <p>
     * Estratégia de Caching:
     * 1. Gera uma chave normalizada (ignorando espaços extras e quebras de linha).
     * 2. Verifica se já existe uma resposta SUCESSO no cache.
     * 3. Se sim, retorna imediatamente (CompletableFuture.completedFuture).
     * 4. Se não, executa a request e, ao terminar com sucesso, salva no cache.
     *
     * @param provider O provedor de IA a ser utilizado via funcionalinterface ou
     *                 objeto
     * @param request  A requisição contendo prompt, system message, etc.
     * @return Future com a resposta (do cache ou nova)
     */
    public CompletableFuture<AIProviderResponse> executeWithCache(
            com.matheusdev.mindforge.ai.provider.AIProvider provider, AIProviderRequest request) {
        String cacheKey = generateCacheKey(request);
        org.springframework.cache.Cache cache = cacheManager.getCache(CacheConfig.AI_PROMPTS_CACHE);

        if (cache != null) {
            AIProviderResponse cachedResponse = cache.get(cacheKey, AIProviderResponse.class);
            if (cachedResponse != null) {
                log.info("🎯 Cache HIT para prompt normalizado. Chave: '{}'", limitLog(cacheKey));
                return CompletableFuture.completedFuture(cachedResponse);
            }
        }

        log.debug("💨 Cache MISS. Executando requisição no provedor...");
        return provider.executeTask(request).thenApply(response -> {
            // Só faz cache se a resposta for válida e não tiver erro
            if (response != null && response.getContent() != null && !response.getContent().isBlank()) {
                if (cache != null) {
                    log.debug("💾 Salvando resposta no cache. Chave: '{}'", limitLog(cacheKey));
                    cache.put(cacheKey, response);
                }
            }
            return response;
        });
    }

    /**
     * Gera uma chave de cache normalizada para aumentar a taxa de acerto.
     * Remove espaços extras, trim e considera o contexto do sistema.
     */
    private String generateCacheKey(AIProviderRequest request) {
        StringBuilder keyBuilder = new StringBuilder();

        // 1. Normaliza Prompt do Usuário (Otimização "Pulo do Gato")
        if (request.textPrompt() != null) {
            keyBuilder.append(normalizeText(request.textPrompt()));
        }

        keyBuilder.append("|");

        // 2. Considera System Message (CRUCIAL para RAG/Contexto diferente)
        if (request.systemMessage() != null) {
            keyBuilder.append(request.systemMessage().hashCode());
        }

        keyBuilder.append("|");

        // 3. Modelo (Se mudar o modelo, a resposta deve ser diferente)
        if (request.model() != null) {
            keyBuilder.append(request.model());
        }

        // 4. Se for imagem ou documento, adiciona hash do conteúdo
        if (request.imageData() != null) {
            keyBuilder.append("|IMG:").append(java.util.Arrays.hashCode(request.imageData()));
        }

        return keyBuilder.toString();
    }

    private String normalizeText(String text) {
        if (text == null)
            return "";
        // Remove espaços duplicados e quebras de linha variadas
        return text.trim().replaceAll("\\s+", " ");
    }

    private String limitLog(String text) {
        if (text == null)
            return "null";
        return text.length() > 50 ? text.substring(0, 50) + "..." : text;
    }
}
