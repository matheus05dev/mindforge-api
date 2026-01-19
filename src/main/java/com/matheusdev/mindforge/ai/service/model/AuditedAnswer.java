package com.matheusdev.mindforge.ai.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuditedAnswer(
    Answer answer,
    List<EvidenceRef> references
) {}
