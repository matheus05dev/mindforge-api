package com.matheusdev.mindforge.core.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    public static final String AI_PROMPTS_CACHE = "ai-prompts";
    public static final String EMBEDDINGS_CACHE = "embeddings";
    public static final String RAG_RETRIEVAL_CACHE = "rag-retrieval";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(AI_PROMPTS_CACHE, EMBEDDINGS_CACHE,
                RAG_RETRIEVAL_CACHE);
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .expireAfterAccess(24, TimeUnit.HOURS) // Aumentado para 24h (embeddings mudam pouco)
                .maximumSize(2000) // Aumentado para 2000 itens
                .recordStats();
    }
}
