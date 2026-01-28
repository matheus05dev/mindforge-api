package com.matheusdev.mindforge.core.tenant.listener;

import com.matheusdev.mindforge.core.tenant.context.TenantContext;
import com.matheusdev.mindforge.core.tenant.model.Tenant;
import jakarta.persistence.PrePersist;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

/**
 * JPA Entity Listener to automatically set the current tenant on entities
 * before persistence.
 */
public class TenantEntityListener {

    @PrePersist
    public void setTenant(Object entity) {
        Long currentTenantId = TenantContext.getTenantId();

        if (currentTenantId == null) {
            // Se não houver tenant no contexto (ex: seed, tests), ignorar ou logar.
            // Para segurança estrita, poderíamos lançar exceção, mas vamos permitir
            // para facilitar seeds iniciais.
            return;
        }

        try {
            BeanWrapper wrapper = new BeanWrapperImpl(entity);

            // Verifica se a entidade tem propriedade "tenant"
            if (wrapper.isWritableProperty("tenant")) {
                Object currentTenantValue = wrapper.getPropertyValue("tenant");

                // Só define se estiver nulo
                if (currentTenantValue == null) {
                    Tenant tenantRef = new Tenant();
                    tenantRef.setId(currentTenantId);
                    wrapper.setPropertyValue("tenant", tenantRef);
                }
            }
        } catch (Exception e) {
            // Ignorar erros se a entidade não for compatível, embora devêssemos usar
            // uma interface marker para evitar reflexão desnecessária.
            // Visto que vamos anotar explicitamente, o erro não deve ocorrer.
            System.err.println("Erro ao definir tenant automático para: " + entity.getClass().getSimpleName() + ": "
                    + e.getMessage());
        }
    }
}
