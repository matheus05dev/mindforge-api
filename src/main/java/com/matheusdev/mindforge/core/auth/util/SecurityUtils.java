package com.matheusdev.mindforge.core.auth.util;

import com.matheusdev.mindforge.core.auth.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class SecurityUtils {

    private SecurityUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Get the currently authenticated user.
     * 
     * @return Optional containing the User object if authenticated, empty
     *         otherwise.
     */
    public static Optional<User> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return Optional.of((User) authentication.getPrincipal());
        }
        return Optional.empty();
    }

    /**
     * Get the tenant ID of the currently authenticated user.
     * 
     * @return Tenant ID if authenticated, throws RuntimeException if not found.
     */
    public static Long getCurrentTenantId() {
        // 1. Try to get from ThreadLocal TenantContext (set by filters or async
        // orchestrators)
        Long contextId = com.matheusdev.mindforge.core.tenant.context.TenantContext.getTenantId();
        if (contextId != null) {
            return contextId;
        }

        // 2. Fallback to Authenticated User Object
        return getAuthenticatedUser()
                .map(User::getTenantId)
                .orElseThrow(() -> new RuntimeException("No authenticated user found or user has no tenant"));
    }

    /**
     * Get the ID of the currently authenticated user.
     * 
     * @return User ID if authenticated, throws RuntimeException if not found.
     */
    public static Long getCurrentUserId() {
        return getAuthenticatedUser()
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("No authenticated user found"));
    }
}
