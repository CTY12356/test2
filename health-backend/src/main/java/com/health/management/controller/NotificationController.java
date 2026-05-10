package com.health.management.controller;

import com.health.management.common.ApiResponse;
import com.health.management.entity.Notification;
import com.health.management.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<List<Notification>> list(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        return ApiResponse.ok(notificationService.listByUser(userId));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        return ApiResponse.ok(Map.of("count", notificationService.countUnread(userId)));
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllRead(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        notificationService.markAllRead(userId);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id, Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        notificationService.markRead(id, userId);
        return ApiResponse.ok(null);
    }
}
