package com.matheusdev.mindforge.ai.provider.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIProviderResponse {
    private String content;
    private String error;
}
