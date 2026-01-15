package com.matheusdev.mindforge.integration.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class UserIntegration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // No futuro, este será o ID do usuário logado
    private Long userId;

    @Enumerated(EnumType.STRING)
    private Provider provider;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String accessToken;

    private String refreshToken;

    public enum Provider {
        GITHUB
    }
}
