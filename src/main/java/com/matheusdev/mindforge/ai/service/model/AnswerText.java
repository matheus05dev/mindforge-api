package com.matheusdev.mindforge.ai.service.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AnswerText(String markdown, String plainText) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public AnswerText(@JsonProperty("markdown") String markdown, @JsonProperty("plainText") String plainText) {
        this.markdown = markdown;
        this.plainText = plainText;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public AnswerText(String content) {
        this(content, content); // Usa o mesmo conteúdo para ambos os campos
    }
}
