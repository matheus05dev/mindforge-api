package com.matheusdev.mindforge.ai.provider;

import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;

public interface AIProvider {
    AIProviderResponse executeTask(AIProviderRequest request);
}
