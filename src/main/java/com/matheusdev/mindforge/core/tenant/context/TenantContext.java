package com.matheusdev.mindforge.core.tenant.context;

/**
 * Thread-local context to store the current tenant ID for the request.
 * This ensures tenant isolation across concurrent requests.
 */
public class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
        // Private constructor to prevent instantiation
    }

    /**
     * Set the current tenant ID for this thread
     */
    public static void setTenantId(Long tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Get the current tenant ID for this thread
     */
    public static Long getTenantId() {
        return CURRENT_TENANT.get();
    }

    /**
     * Clear the tenant context for this thread.
     * Should be called after request processing to prevent memory leaks.
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
