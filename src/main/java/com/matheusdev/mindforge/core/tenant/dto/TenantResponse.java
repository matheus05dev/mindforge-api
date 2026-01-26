package com.matheusdev.mindforge.core.tenant.dto;

import com.matheusdev.mindforge.core.tenant.domain.TenantPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantResponse {
    private Long id;
    private String name;
    private String slug;
    private Boolean active;
    private TenantPlan plan;
    private Integer maxUsers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
