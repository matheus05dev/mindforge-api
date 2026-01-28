package com.matheusdev.mindforge.core.auth.dto;

import com.matheusdev.mindforge.core.auth.domain.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private Role role;
    private Long tenantId;
    private boolean isGithubConnected;
}
