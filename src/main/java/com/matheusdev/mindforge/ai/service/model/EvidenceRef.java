package com.matheusdev.mindforge.ai.service.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record EvidenceRef(int evidenceId, String text) {
    @JsonCreator
    public EvidenceRef(@JsonProperty("evidenceId") int evidenceId, @JsonProperty("text") String text) {
        this.evidenceId = evidenceId;
        this.text = text;
    }
}
