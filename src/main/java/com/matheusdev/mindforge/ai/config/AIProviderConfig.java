package com.matheusdev.mindforge.ai.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class AIProviderConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // Configura timeouts HTTP adequados para o Ollama
        // Connect timeout: tempo para estabelecer conexão (5 segundos)
        // Read timeout: tempo para ler resposta (180 segundos - dá tempo ao Ollama processar)
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(180))
                .build();
    }
}
