package com.matheusdev.mindforge.core.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusdev.mindforge.core.auth.dto.AuthenticationRequest;
import com.matheusdev.mindforge.core.auth.dto.AuthenticationResponse;
import com.matheusdev.mindforge.core.auth.dto.RegisterRequest;
import com.matheusdev.mindforge.core.auth.dto.UserResponse;
import com.matheusdev.mindforge.core.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for unit testing
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/auth/me should return user profile")
    void getMe_ShouldReturnUserProfile() throws Exception {
        // Arrange
        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .isGithubConnected(true)
                .build();

        when(authService.getUserProfile()).thenReturn(userResponse);

        // Act & Assert
        mockMvc.perform(get("/api/auth/me")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.githubConnected").value(true));
    }

    @Test
    @DisplayName("POST /api/auth/authenticate should return token")
    void authenticate_ShouldReturnToken() throws Exception {
        // Arrange
        AuthenticationRequest request = new AuthenticationRequest("test@example.com", "password");
        AuthenticationResponse response = AuthenticationResponse.builder().token("jwt-token").build();

        when(authService.authenticate(any(AuthenticationRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    @DisplayName("POST /api/auth/register should return token")
    void register_ShouldReturnToken() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest("John", "Doe", "john@example.com", "password");
        AuthenticationResponse response = AuthenticationResponse.builder().token("jwt-token").build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }
}
