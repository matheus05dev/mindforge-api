package com.matheusdev.mindforge.knowledgeltem.dto;

import com.matheusdev.mindforge.document.dto.DocumentResponse;
import lombok.Data;
import java.util.List;

@Data
public class KnowledgeItemResponse {
    private Long id;
    private String title;
    private String content;
    private List<String> tags;
    private List<DocumentResponse> documents;
}
