package com.matheusdev.mindforge.ai.service.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record Answer(String markdown, String plainText) {
    @JsonCreator
    public Answer(@JsonProperty("markdown") String markdown, @JsonProperty("plainText") String plainText) {
        this.markdown = markdown;
        this.plainText = plainText;
    }
}
