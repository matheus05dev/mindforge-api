package com.matheusdev.mindforge.core.auth.service;

import com.matheusdev.mindforge.core.auth.config.JwtService;
import com.matheusdev.mindforge.core.auth.domain.Role;
import com.matheusdev.mindforge.core.auth.domain.User;
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

        public AuthenticationResponse register(RegisterRequest request) {
                var user = User.builder()
                                .name(request.getFirstname() + " " + request.getLastname())
                                .email(request.getEmail())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .role(Role.USER)
                                .build();
                User savedUser = userRepository.save(user);

                // Initialize UserProfileAI for the new user
                UserProfileAI userProfile = new UserProfileAI();
                userProfile.setId(savedUser.getId());
                userProfile.setSummary("Novo usuário cadastrado via sistema de autenticação.");
                userProfile.setLearningStyle(LearningStyle.PRACTICAL);
                userProfile.setCommunicationTone(CommunicationTone.ENCOURAGING);
                userProfileAIRepository.save(userProfile);

                var jwtToken = jwtService.generateToken(user);
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
                var jwtToken = jwtService.generateToken(user);
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
