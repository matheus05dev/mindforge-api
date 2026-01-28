package com.matheusdev.mindforge.core.auth.util;

import com.matheusdev.mindforge.core.auth.model.User;
import com.matheusdev.mindforge.core.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class SecurityUtilsTest {

    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        SecurityContextHolder.setContext(securityContext);
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should prefer TenantContext over SecurityContext if available")
    void getCurrentTenantId_ShouldPreferTenantContext() {
        // Arrange
        TenantContext.setTenantId(99L);

        // Mock user with different tenant ID to ensure preference logic works
        User user = User.builder().id(1L).tenantId(10L).build();
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);

        // Act
        Long tenantId = SecurityUtils.getCurrentTenantId();

        // Assert
        assertEquals(99L, tenantId);
    }

    @Test
    @DisplayName("Should fallback to SecurityContext if TenantContext is empty")
    void getCurrentTenantId_ShouldFallbackToSecurityContext() {
        // Arrange
        User user = User.builder().id(1L).tenantId(10L).build();
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);

        // Act
        Long tenantId = SecurityUtils.getCurrentTenantId();

        // Assert
        assertEquals(10L, tenantId);
    }

    @Test
    @DisplayName("Should throw exception if both are missing")
    void getCurrentTenantId_ShouldThrowExceptionSinceEmpty() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(null);

        // Act & Assert
        assertThrows(RuntimeException.class, SecurityUtils::getCurrentTenantId);
    }
}
