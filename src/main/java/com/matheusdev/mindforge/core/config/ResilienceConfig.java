package com.matheusdev.mindforge.core.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    public static final String GROQ_INSTANCE = "groqProvider";
    public static final String OLLAMA_INSTANCE = "ollamaProvider";
    public static final String GITHUB_CLIENT_INSTANCE = "githubClient";

    @Bean
    public RetryRegistry retryRegistry() {
        // Configuração padrão para o Ollama e outros.
        RetryConfig defaultConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .build();

        RetryRegistry registry = RetryRegistry.of(defaultConfig);

        // Configuração específica para o Groq para lidar com a limitação de taxa (429)
        RetryConfig groqProviderConfig = RetryConfig.custom()
                .maxAttempts(4)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(
                        Duration.ofSeconds(7),
                        2.0
                ))
                .retryExceptions(HttpClientErrorException.TooManyRequests.class)
                .failAfterMaxAttempts(true)
                .build();

        registry.addConfiguration(GROQ_INSTANCE, groqProviderConfig);
        // A configuração do Ollama usará o 'defaultConfig'
        registry.addConfiguration(OLLAMA_INSTANCE, defaultConfig);


        return registry;
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(10000))
                .slidingWindowSize(10)
                .build();
        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(10)
                .timeoutDuration(Duration.ofMillis(500))
                .build();
        return RateLimiterRegistry.of(config);
    }

    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterConfig groqConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(60)) // Timeout padrão para Groq
                .build();

        // Timeout aumentado para 180 segundos para dar tempo ao Ollama processar
        TimeLimiterConfig ollamaConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(180))
                .build();

        TimeLimiterRegistry registry = TimeLimiterRegistry.of(groqConfig);
        registry.addConfiguration(OLLAMA_INSTANCE, ollamaConfig);

        return registry;
    }
}
