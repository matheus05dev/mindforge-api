package com.matheusdev.mindforge.knowledgeltem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "Request to create or update a knowledge item")
public class KnowledgeItemRequest {
    @Schema(description = "The title of the knowledge item", example = "Java Best Practices")
    private String title;
    @Schema(description = "The content of the knowledge item", example = "Use streams for collections")
    private String content;
    @Schema(description = "The tags of the knowledge item", example = "[\"java\", \"best-practices\"]")
    private List<String> tags;
}
