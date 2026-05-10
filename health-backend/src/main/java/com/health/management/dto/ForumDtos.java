package com.health.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

public class ForumDtos {
    @Data
    public static class PostRequest {
        @NotBlank
        private String title;
        @NotBlank
        private String content;
        @NotBlank
        private String category;
        private String tags;
        private String imageUrl;
    }

    @Data
    public static class CommentRequest {
        @NotNull
        private Long postId;
        private Long parentId;
        @NotBlank
        private String content;
    }

    @Data
    public static class ReportRequest {
        @NotBlank
        private String targetType;
        @NotNull
        private Long targetId;
        @NotBlank
        private String reason;
    }
}
