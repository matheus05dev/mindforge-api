package com.matheusdev.mindforge.core.auth.controller;

import com.matheusdev.mindforge.core.auth.dto.AuthenticationRequest;
import com.matheusdev.mindforge.core.auth.dto.AuthenticationResponse;
import com.matheusdev.mindforge.core.auth.dto.RegisterRequest;
import com.matheusdev.mindforge.core.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(service.register(request));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(service.authenticate(request));
    }

    @org.springframework.web.bind.annotation.GetMapping("/me")
    public ResponseEntity<com.matheusdev.mindforge.core.auth.dto.UserResponse> getMe() {
        return ResponseEntity.ok(service.getUserProfile());
    }

    @org.springframework.web.bind.annotation.PatchMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody com.matheusdev.mindforge.core.auth.dto.ChangePasswordRequest request) {
        service.changePassword(request, service.getCurrentUser());
        return ResponseEntity.noContent().build();
    }
}
