package com.matheusdev.mindforge.core.tenant.controller;

import com.matheusdev.mindforge.core.auth.service.AuthService;
import com.matheusdev.mindforge.core.tenant.model.Tenant;
import com.matheusdev.mindforge.core.tenant.dto.TenantResponse;
import com.matheusdev.mindforge.core.tenant.dto.TenantUpdateRequest;
import com.matheusdev.mindforge.core.tenant.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenant Management", description = "APIs for managing tenant/organization settings")
public class TenantController {

    private final TenantService tenantService;
    private final AuthService authService;

    @GetMapping("/current")
    @Operation(summary = "Get current tenant information")
    public ResponseEntity<TenantResponse> getCurrentTenant() {
        var currentUser = authService.getCurrentUser();
        Tenant tenant = tenantService.getTenantById(currentUser.getTenantId());

        return ResponseEntity.ok(mapToResponse(tenant));
    }

    @PutMapping("/current")
    @Operation(summary = "Update current tenant settings")
    public ResponseEntity<TenantResponse> updateCurrentTenant(
            @Valid @RequestBody TenantUpdateRequest request) {
        var currentUser = authService.getCurrentUser();

        Tenant tenantUpdate = Tenant.builder()
                .name(request.getName())
                .active(request.getActive())
                .plan(request.getPlan())
                .maxUsers(request.getMaxUsers())
                .build();

        Tenant updated = tenantService.updateTenant(currentUser.getTenantId(), tenantUpdate);
        return ResponseEntity.ok(mapToResponse(updated));
    }

    private TenantResponse mapToResponse(Tenant tenant) {
        return TenantResponse.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .slug(tenant.getSlug())
                .active(tenant.getActive())
                .plan(tenant.getPlan())
                .maxUsers(tenant.getMaxUsers())
                .createdAt(tenant.getCreatedAt())
                .updatedAt(tenant.getUpdatedAt())
                .build();
    }
}
