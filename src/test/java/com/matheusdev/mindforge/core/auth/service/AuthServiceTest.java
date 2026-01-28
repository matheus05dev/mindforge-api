package com.matheusdev.mindforge.core.auth.service;

import com.matheusdev.mindforge.ai.memory.model.UserProfileAI;
import com.matheusdev.mindforge.ai.memory.repository.UserProfileAIRepository;
import com.matheusdev.mindforge.core.auth.config.JwtService;
import com.matheusdev.mindforge.core.auth.model.Role;
import com.matheusdev.mindforge.core.auth.model.User;
import com.matheusdev.mindforge.core.auth.dto.AuthenticationResponse;
import com.matheusdev.mindforge.core.auth.dto.RegisterRequest;
import com.matheusdev.mindforge.core.auth.dto.UserResponse;
import com.matheusdev.mindforge.core.auth.repository.UserRepository;
import com.matheusdev.mindforge.core.config.DataSeeder;
import com.matheusdev.mindforge.core.tenant.model.Tenant;
import com.matheusdev.mindforge.core.tenant.repository.TenantRepository;
import com.matheusdev.mindforge.integration.model.UserIntegration;
import com.matheusdev.mindforge.integration.repository.UserIntegrationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserProfileAIRepository userProfileAIRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserIntegrationRepository userIntegrationRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private DataSeeder dataSeeder;

    @InjectMocks
    private AuthService authService;

    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @Test
    @DisplayName("Should return user profile with correct GitHub status and Name fallback")
    void getUserProfile_ShouldReturnCorrectData() {
        // Arrange
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .role(Role.USER)
                .tenantId(10L)
                .build(); // Name is null

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("test@example.com");
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userIntegrationRepository.findByUserIdAndProvider(1L, UserIntegration.Provider.GITHUB))
                .thenReturn(Optional.of(new UserIntegration()));

        // Act
        UserResponse response = authService.getUserProfile();

        // Assert
        assertNotNull(response);
        assertEquals("Test", response.getName()); // Should fallback to "Test" from "test@example.com"
        assertTrue(response.isGithubConnected());
        assertEquals(1L, response.getId());
    }

    @Test
    @DisplayName("Should return 'Usuário' when name is null and email is invalid for fallback")
    void getUserProfile_ShouldFallbackToUsuario() {
        // Arrange
        User user = User.builder()
                .id(1L)
                .email("invalid-email")
                .build();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("invalid-email");
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail("invalid-email")).thenReturn(Optional.of(user));
        when(userIntegrationRepository.findByUserIdAndProvider(anyLong(), any())).thenReturn(Optional.empty());

        // Act
        UserResponse response = authService.getUserProfile();

        // Assert
        assertEquals("Usuário", response.getName());
        assertFalse(response.isGithubConnected());
    }

    @Test
    @DisplayName("Should register new user and create tenant")
    void register_ShouldCreateUserAndTenant() {
        // Arrange
        RegisterRequest request = new RegisterRequest("John", "Doe", "john@example.com", "password");
        Tenant tenant = Tenant.builder().id(1L).name("John's Workspace").build();
        User savedUser = User.builder().id(1L).email("john@example.com").tenant(tenant).build();

        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);
        when(passwordEncoder.encode("password")).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(User.class), anyLong())).thenReturn("jwt-token");

        // Act
        AuthenticationResponse response = authService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        verify(tenantRepository).save(any(Tenant.class));
        verify(userRepository).save(any(User.class));
        verify(dataSeeder).seedWorkspacesForTenant(any(Tenant.class));
        verify(userProfileAIRepository).save(any(UserProfileAI.class));
    }
}
