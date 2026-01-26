package com.matheusdev.mindforge.core.tenant.filter;

import com.matheusdev.mindforge.core.auth.config.JwtService;
import com.matheusdev.mindforge.core.tenant.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to extract tenant ID from JWT token and populate TenantContext.
 * Executes after JWT authentication to ensure user is authenticated.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            // Extract JWT token from Authorization header
            final String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                final String jwt = authHeader.substring(7);

                // Extract tenant ID from token
                final Long tenantId = jwtService.extractTenantId(jwt);

                if (tenantId != null) {
                    TenantContext.setTenantId(tenantId);
                    log.debug("Tenant context set to: {}", tenantId);
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            // Always clear context after request to prevent memory leaks
            TenantContext.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip filter for public endpoints
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/") ||
                path.startsWith("/login/") ||
                path.startsWith("/oauth2/") ||
                path.startsWith("/api-docs/") ||
                path.startsWith("/swagger-ui/");
    }
}
