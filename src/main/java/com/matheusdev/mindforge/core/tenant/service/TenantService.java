package com.matheusdev.mindforge.core.tenant.service;

import com.matheusdev.mindforge.core.tenant.domain.Tenant;
import com.matheusdev.mindforge.core.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public Tenant getTenantById(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Tenant getTenantBySlug(String slug) {
        return tenantRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Tenant not found with slug: " + slug));
    }

    @Transactional
    public Tenant createTenant(Tenant tenant) {
        if (tenantRepository.existsBySlug(tenant.getSlug())) {
            throw new IllegalArgumentException("Tenant with slug '" + tenant.getSlug() + "' already exists");
        }

        log.info("Creating new tenant: {}", tenant.getName());
        return tenantRepository.save(tenant);
    }

    @Transactional
    public Tenant updateTenant(Long id, Tenant tenantUpdate) {
        Tenant existingTenant = getTenantById(id);

        existingTenant.setName(tenantUpdate.getName());
        existingTenant.setActive(tenantUpdate.getActive());
        existingTenant.setPlan(tenantUpdate.getPlan());
        existingTenant.setMaxUsers(tenantUpdate.getMaxUsers());

        log.info("Updated tenant: {}", existingTenant.getName());
        return tenantRepository.save(existingTenant);
    }

    @Transactional
    public void deleteTenant(Long id) {
        Tenant tenant = getTenantById(id);

        if (id == 1L) {
            throw new IllegalStateException("Cannot delete default tenant");
        }

        log.warn("Deleting tenant: {}", tenant.getName());
        tenantRepository.delete(tenant);
    }
}
