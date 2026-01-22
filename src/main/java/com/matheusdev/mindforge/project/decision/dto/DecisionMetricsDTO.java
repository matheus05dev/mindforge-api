package com.matheusdev.mindforge.project.decision.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DecisionMetricsDTO {
    private long totalDecisions;
    private long acceptedCount;
    private long proposedCount;
    private long rejectedCount;
    private long deprecatedCount;
    private double acceptanceRate;
    private double volatilityScore; // Ex: (Rejected + Deprecated) / Total
}
