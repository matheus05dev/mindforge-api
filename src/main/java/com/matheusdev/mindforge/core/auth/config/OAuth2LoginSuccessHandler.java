package com.matheusdev.mindforge.core.auth.config;

import com.matheusdev.mindforge.core.auth.domain.Role;
import com.matheusdev.mindforge.core.auth.domain.User;
import com.matheusdev.mindforge.core.auth.repository.UserRepository;
import com.matheusdev.mindforge.core.tenant.domain.Tenant;
import com.matheusdev.mindforge.core.tenant.domain.TenantPlan;
import com.matheusdev.mindforge.core.tenant.repository.TenantRepository;
import com.matheusdev.mindforge.core.config.DataSeeder;
import com.matheusdev.mindforge.ai.memory.model.UserProfileAI;
import com.matheusdev.mindforge.ai.memory.repository.UserProfileAIRepository;
import com.matheusdev.mindforge.ai.memory.model.LearningStyle;
import com.matheusdev.mindforge.ai.memory.model.CommunicationTone;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserProfileAIRepository userProfileAIRepository;
    private final TenantRepository tenantRepository;
    private final DataSeeder dataSeeder;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        // Check if email is null (GitHub sometimes returns null email if private)
        if (email == null) {
            email = oAuth2User.getAttribute("login") + "@github.com"; // Fallback to login
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
        } else {
            // Create unique tenant for new OAuth user
            String tenantName = (name != null ? name : "GitHub User") + "'s Workspace";
            String baseSlug = (name != null ? name : "user").toLowerCase().replaceAll("\\s+", "-");
            String uniqueSlug = baseSlug + "-" + java.util.UUID.randomUUID().toString().substring(0, 8);

            Tenant newTenant = Tenant.builder()
                    .name(tenantName)
                    .slug(uniqueSlug)
                    .active(true)
                    .plan(TenantPlan.FREE)
                    .maxUsers(1)
                    .build();

            newTenant = tenantRepository.save(newTenant);

            // Set context for user creation
            com.matheusdev.mindforge.core.tenant.context.TenantContext.setTenantId(newTenant.getId());

            try {
                // Create new user with tenant
                user = User.builder()
                        .email(email)
                        .name(name != null ? name : "GitHub User")
                        .role(Role.USER)
                        .password("") // No password for OAuth users
                        .tenant(newTenant)
                        .build();
                user = userRepository.save(user);

                // Seed default workspaces for this tenant
                dataSeeder.seedWorkspacesForTenant(newTenant);

                // Initialize UserProfileAI
                UserProfileAI userProfile = new UserProfileAI();
                userProfile.setId(user.getId());
                userProfile.setSummary("Usuário autenticado via GitHub.");
                userProfile.setLearningStyle(LearningStyle.PRACTICAL);
                userProfile.setCommunicationTone(CommunicationTone.ENCOURAGING);
                userProfileAIRepository.save(userProfile);
            } finally {
                com.matheusdev.mindforge.core.tenant.context.TenantContext.clear();
            }
        }

        // Generate JWT with tenant_id
        String token = jwtService.generateToken(user, user.getTenant().getId());

        // Redirect to Frontend Callback URL with Token
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/callback")
                .queryParam("token", token)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
