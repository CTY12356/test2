package com.health.management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public class AuthDtos {
    @Data
    public static class RegisterRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
        private String nickname;
    }

    @Data
    public static class LoginRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
    }

    @Data
    public static class AuthResponse {
        private final String token;
        private final Long userId;
        private final String username;
        private final String nickname;
        private final String role;
        private final String avatarUrl;
    }
}
