package com.matheusdev.mindforge.core.auth.service;

import com.matheusdev.mindforge.core.auth.config.JwtService;
import com.matheusdev.mindforge.core.auth.model.Role;
import com.matheusdev.mindforge.core.auth.model.User;
import com.matheusdev.mindforge.core.auth.dto.AuthenticationRequest;
import com.matheusdev.mindforge.core.auth.dto.AuthenticationResponse;
import com.matheusdev.mindforge.core.auth.dto.RegisterRequest;
import com.matheusdev.mindforge.core.auth.repository.UserRepository;
import com.matheusdev.mindforge.ai.memory.model.UserProfileAI;
import com.matheusdev.mindforge.ai.memory.repository.UserProfileAIRepository;
import com.matheusdev.mindforge.ai.memory.model.LearningStyle;
import com.matheusdev.mindforge.ai.memory.model.CommunicationTone;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

        private final UserRepository userRepository;
        private final UserProfileAIRepository userProfileAIRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final AuthenticationManager authenticationManager;

        private final com.matheusdev.mindforge.integration.repository.UserIntegrationRepository userIntegrationRepository;
        private final com.matheusdev.mindforge.core.tenant.repository.TenantRepository tenantRepository;

        private final com.matheusdev.mindforge.core.config.DataSeeder dataSeeder;

        @org.springframework.transaction.annotation.Transactional
        public AuthenticationResponse register(RegisterRequest request) {
                // Create a unique tenant for the new user
                String tenantName = request.getFirstname() + "'s Workspace";
                String baseSlug = (request.getFirstname() + "-" + request.getLastname()).toLowerCase()
                                .replaceAll("\\s+", "-");
                String uniqueSlug = baseSlug + "-" + java.util.UUID.randomUUID().toString().substring(0, 8);

                var newTenant = com.matheusdev.mindforge.core.tenant.model.Tenant.builder()
                                .name(tenantName)
                                .slug(uniqueSlug)
                                .active(true)
                                .plan(com.matheusdev.mindforge.core.tenant.model.TenantPlan.FREE)
                                .maxUsers(1)
                                .build();

                newTenant = tenantRepository.save(newTenant);

                var user = User.builder()
                                .name(request.getFirstname() + " " + request.getLastname())
                                .email(request.getEmail())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .role(Role.USER)
                                .tenant(newTenant) // Assign the NEW tenant
                                .build();

                // Set context for saving user and seeding data
                com.matheusdev.mindforge.core.tenant.context.TenantContext.setTenantId(newTenant.getId());

                User savedUser;
                try {
                        savedUser = userRepository.save(user);

                        // Seed default workspaces for this new tenant
                        dataSeeder.seedWorkspacesForTenant(newTenant);

                } finally {
                        com.matheusdev.mindforge.core.tenant.context.TenantContext.clear();
                }

                // Initialize UserProfileAI for the new user
                UserProfileAI userProfile = new UserProfileAI();
                userProfile.setId(savedUser.getId());
                userProfile.setSummary("Novo usuário cadastrado via sistema de autenticação.");
                userProfile.setLearningStyle(LearningStyle.PRACTICAL);
                userProfile.setCommunicationTone(CommunicationTone.ENCOURAGING);
                userProfileAIRepository.save(userProfile);

                var jwtToken = jwtService.generateToken(user, user.getTenant().getId());
                return AuthenticationResponse.builder()
                                .token(jwtToken)
                                .build();
        }

        public AuthenticationResponse authenticate(AuthenticationRequest request) {
                authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                request.getEmail(),
                                                request.getPassword()));
                var user = userRepository.findByEmail(request.getEmail())
                                .orElseThrow();
                var jwtToken = jwtService.generateToken(user, user.getTenantId());
                return AuthenticationResponse.builder()
                                .token(jwtToken)
                                .build();
        }

        public User getCurrentUser() {
                var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext()
                                .getAuthentication();
                if (authentication == null || !authentication.isAuthenticated()) {
                        throw new RuntimeException("User not authenticated");
                }
                String email;
                if (authentication
                                .getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User oauthUser) {
                        email = oauthUser.getAttribute("email");
                        if (email == null) {
                                email = oauthUser.getAttribute("login") + "@github.com";
                        }
                } else if (authentication
                                .getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                        email = userDetails.getUsername();
                } else {
                        email = authentication.getName();
                }

                return userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));
        }

        public com.matheusdev.mindforge.core.auth.dto.UserResponse getUserProfile() {
                User user = getCurrentUser();
                boolean isGithubConnected = userIntegrationRepository
                                .findByUserIdAndProvider(user.getId(),
                                                com.matheusdev.mindforge.integration.model.UserIntegration.Provider.GITHUB)
                                .isPresent();

                String displayName = user.getName();
                if (displayName == null || displayName.trim().isEmpty()) {
                        String email = user.getEmail();
                        if (email != null && email.contains("@")) {
                                displayName = email.substring(0, email.indexOf("@"));
                                // Capitalize first letter
                                if (displayName.length() > 0) {
                                        displayName = displayName.substring(0, 1).toUpperCase()
                                                        + displayName.substring(1);
                                }
                        } else {
                                displayName = "Usuário";
                        }
                }

                return com.matheusdev.mindforge.core.auth.dto.UserResponse.builder()
                                .id(user.getId())
                                .name(displayName)
                                .email(user.getEmail())
                                .role(user.getRole())
                                .tenantId(user.getTenantId())
                                .isGithubConnected(isGithubConnected)
                                .build();
        }

        public void changePassword(com.matheusdev.mindforge.core.auth.dto.ChangePasswordRequest request, User user) {
                // check if the current password is correct (if user has one set)
                if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                                throw new IllegalStateException("Senha atual incorreta");
                        }
                }

                // check if the two new passwords are the same
                if (!request.getNewPassword().equals(request.getConfirmationPassword())) {
                        throw new IllegalStateException("As senhas não coincidem");
                }

                // update the password
                user.setPassword(passwordEncoder.encode(request.getNewPassword()));

                // save the new password
                userRepository.save(user);
        }
}
