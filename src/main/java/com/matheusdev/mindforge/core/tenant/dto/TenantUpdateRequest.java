package com.matheusdev.mindforge.core.tenant.dto;

import com.matheusdev.mindforge.core.tenant.domain.TenantPlan;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantUpdateRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Active status is required")
    private Boolean active;

    private TenantPlan plan;

    private Integer maxUsers;
}
