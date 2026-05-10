package com.health.management.controller;

import com.health.management.common.ApiResponse;
import com.health.management.dto.CommentVO;
import com.health.management.dto.ForumDtos.CommentRequest;
import com.health.management.dto.ForumDtos.PostRequest;
import com.health.management.dto.ForumDtos.ReportRequest;
import com.health.management.dto.PostVO;
import com.health.management.entity.Comment;
import com.health.management.entity.Post;
import com.health.management.entity.Report;
import com.health.management.service.ForumService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/forum")
public class ForumController {
    private final ForumService forumService;

    public ForumController(ForumService forumService) {
        this.forumService = forumService;
    }

    @GetMapping("/posts")
    public ApiResponse<List<PostVO>> listPosts(Authentication authentication,
                                               @RequestParam(required = false) String category,
                                               @RequestParam(required = false) String keyword) {
        Long currentUserId = authentication != null ? userId(authentication) : null;
        return ApiResponse.ok(forumService.listPosts(currentUserId, category, keyword));
    }

    @PostMapping("/posts")
    public ApiResponse<Post> createPost(Authentication authentication,
                                        @Valid @RequestBody PostRequest request) {
        return ApiResponse.ok(forumService.createPost(userId(authentication), request));
    }

    @PostMapping("/posts/{id}/like")
    public ApiResponse<Void> toggleLike(Authentication authentication, @PathVariable Long id) {
        forumService.toggleLike(userId(authentication), id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/posts/{id}/comments")
    public ApiResponse<List<CommentVO>> listComments(@PathVariable Long id) {
        return ApiResponse.ok(forumService.listComments(id));
    }

    @PostMapping("/comments")
    public ApiResponse<Comment> createComment(Authentication authentication,
                                              @Valid @RequestBody CommentRequest request) {
        return ApiResponse.ok(forumService.createComment(userId(authentication), request));
    }

    @PostMapping("/reports")
    public ApiResponse<Report> report(Authentication authentication,
                                      @Valid @RequestBody ReportRequest request) {
        return ApiResponse.ok(forumService.report(userId(authentication), request));
    }

    private Long userId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
