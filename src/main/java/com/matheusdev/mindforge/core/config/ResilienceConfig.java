package com.matheusdev.mindforge.core.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    public static final String AI_PROVIDER_INSTANCE = "aiProvider";
    public static final String GITHUB_CLIENT_INSTANCE = "githubClient";

    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .build();
        return RetryRegistry.of(config);
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
        // Timeout aumentado para 180 segundos para dar tempo ao Ollama processar
        // especialmente na primeira chamada quando o modelo precisa ser carregado
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(180))
                .build();
        return TimeLimiterRegistry.of(config);
    }
}
