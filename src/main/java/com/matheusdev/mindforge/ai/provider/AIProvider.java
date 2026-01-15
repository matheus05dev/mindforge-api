package com.matheusdev.mindforge.ai.provider;

import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;

import java.util.concurrent.CompletableFuture;

public interface AIProvider {
    CompletableFuture<AIProviderResponse> executeTask(AIProviderRequest request);
}
