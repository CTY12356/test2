package com.health.management.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostVO {
    private Long id;
    private Long userId;
    private String authorNickname;
    private String authorAvatarUrl;
    private String title;
    private String content;
    private String category;
    private String tags;
    private String imageUrl;
    private Integer likeCount;
    private Integer commentCount;
    private Integer viewCount;
    private String auditStatus;
    private LocalDateTime createdAt;
    private boolean likedByCurrentUser;
}
