package com.matheusdev.mindforge.ai.provider.dto;

import com.matheusdev.mindforge.ai.service.model.Evidence;
import com.matheusdev.mindforge.ai.service.model.InteractionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIProviderResponse {
    private String content;
    private Long sessionId;
    private String error;
    private List<Evidence> evidences;
    private InteractionType type;
}
