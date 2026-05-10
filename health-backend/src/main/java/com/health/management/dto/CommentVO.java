package com.health.management.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentVO {
    private Long id;
    private Long postId;
    private Long userId;
    private String authorNickname;
    private String authorAvatarUrl;
    private Long parentId;
    private String content;
    private Integer likeCount;
    private LocalDateTime createdAt;
}
