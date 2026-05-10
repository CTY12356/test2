package com.health.management.service;

import com.health.management.dto.UserDtos.UserProfileResponse;
import com.health.management.entity.Comment;
import com.health.management.entity.Post;
import com.health.management.entity.Report;
import com.health.management.dto.PostVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {
    private final UserService userService;
    private final ForumService forumService;

    public AdminService(UserService userService, ForumService forumService) {
        this.userService = userService;
        this.forumService = forumService;
    }

    public List<UserProfileResponse> listUsers() {
        return userService.listAll();
    }

    public UserProfileResponse banUser(Long userId) {
        return userService.setStatus(userId, "BANNED");
    }

    public UserProfileResponse unbanUser(Long userId) {
        return userService.setStatus(userId, "ACTIVE");
    }

    public List<PostVO> listAllPosts() {
        return forumService.listAllPosts();
    }

    public Post auditPost(Long id, String status) {
        return forumService.auditPost(id, status);
    }

    public Comment auditComment(Long id, String status) {
        return forumService.auditComment(id, status);
    }

    public void deletePost(Long id) {
        forumService.deletePost(id);
    }

    public void deleteComment(Long id) {
        forumService.deleteComment(id);
    }

    public List<Report> listReports() {
        return forumService.listReports();
    }

    public Report handleReport(Long id, Long adminId, String action, String result) {
        return forumService.handleReport(id, adminId, action, result);
    }
}
