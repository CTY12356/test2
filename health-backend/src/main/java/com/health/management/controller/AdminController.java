package com.health.management.controller;

import com.health.management.common.ApiResponse;
import com.health.management.dto.PostVO;
import com.health.management.dto.UserDtos.UserProfileResponse;
import com.health.management.entity.Comment;
import com.health.management.entity.Post;
import com.health.management.entity.Report;
import com.health.management.service.AdminService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ApiResponse<List<UserProfileResponse>> listUsers(Authentication authentication) {
        requireAdmin(authentication);
        return ApiResponse.ok(adminService.listUsers());
    }

    @PatchMapping("/users/{id}/ban")
    public ApiResponse<UserProfileResponse> banUser(Authentication authentication, @PathVariable Long id) {
        requireAdmin(authentication);
        return ApiResponse.ok(adminService.banUser(id));
    }

    @PatchMapping("/users/{id}/unban")
    public ApiResponse<UserProfileResponse> unbanUser(Authentication authentication, @PathVariable Long id) {
        requireAdmin(authentication);
        return ApiResponse.ok(adminService.unbanUser(id));
    }

    @GetMapping("/posts")
    public ApiResponse<List<PostVO>> listAllPosts(Authentication authentication) {
        requireAdmin(authentication);
        return ApiResponse.ok(adminService.listAllPosts());
    }

    /** POST/PUT/PATCH 均可：避免部分环境对 PUT、PATCH 返回 405 */
    @RequestMapping(value = "/posts/{id}/audit", method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH})
    public ApiResponse<Post> auditPost(Authentication authentication, @PathVariable Long id, @RequestParam String status) {
        requireAdmin(authentication);
        return ApiResponse.ok(adminService.auditPost(id, status));
    }

    @RequestMapping(value = "/comments/{id}/audit", method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH})
    public ApiResponse<Comment> auditComment(Authentication authentication, @PathVariable Long id, @RequestParam String status) {
        requireAdmin(authentication);
        return ApiResponse.ok(adminService.auditComment(id, status));
    }

    @DeleteMapping("/posts/{id}")
    public ApiResponse<Void> deletePost(Authentication authentication, @PathVariable Long id) {
        requireAdmin(authentication);
        adminService.deletePost(id);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/comments/{id}")
    public ApiResponse<Void> deleteComment(Authentication authentication, @PathVariable Long id) {
        requireAdmin(authentication);
        adminService.deleteComment(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/reports")
    public ApiResponse<List<Report>> listReports(Authentication authentication) {
        requireAdmin(authentication);
        return ApiResponse.ok(adminService.listReports());
    }

    @PatchMapping("/reports/{id}/handle")
    public ApiResponse<Report> handleReport(Authentication authentication,
                                            @PathVariable Long id,
                                            @RequestParam String action,
                                            @RequestParam(required = false, defaultValue = "") String result) {
        requireAdmin(authentication);
        Long adminId = Long.valueOf(authentication.getName());
        return ApiResponse.ok(adminService.handleReport(id, adminId, action, result));
    }

    private void requireAdmin(Authentication authentication) {
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equalsIgnoreCase(authority.getAuthority()));
        if (!isAdmin) {
            throw new AccessDeniedException("需要管理员权限");
        }
    }
}
