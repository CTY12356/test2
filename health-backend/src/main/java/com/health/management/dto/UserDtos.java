package com.health.management.dto;

import lombok.Data;

public class UserDtos {
    @Data
    public static class UpdateProfileRequest {
        private String nickname;
        private String avatarUrl;
    }

    @Data
    public static class UserProfileResponse {
        private Long id;
        private String username;
        private String nickname;
        private String avatarUrl;
        private String role;
        private String status;
    }
}
