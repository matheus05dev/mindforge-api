package com.matheusdev.mindforge.knowledgeltem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KnowledgeAIResponse {
    private String result;
    private boolean success;
    private String message;
}
