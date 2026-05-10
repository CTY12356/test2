package com.health.management.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.health.management.entity.Notification;
import com.health.management.mapper.NotificationMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {
    private final NotificationMapper notificationMapper;

    public NotificationService(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    public void create(Long userId, String type, String title, String content,
                       String relatedType, Long relatedId, Long senderId) {
        if (userId == null || userId.equals(senderId)) return; // 不给自己发通知
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setRelatedType(relatedType);
        n.setRelatedId(relatedId);
        n.setSenderId(senderId);
        n.setIsRead(false);
        notificationMapper.insert(n);
    }

    public List<Notification> listByUser(Long userId) {
        return notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getCreatedAt)
                        .last("LIMIT 50"));
    }

    public long countUnread(Long userId) {
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, false));
    }

    public void markAllRead(Long userId) {
        notificationMapper.update(null,
                Wrappers.<Notification>lambdaUpdate()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, false)
                        .set(Notification::getIsRead, true));
    }

    public void markRead(Long id, Long userId) {
        notificationMapper.update(null,
                Wrappers.<Notification>lambdaUpdate()
                        .eq(Notification::getId, id)
                        .eq(Notification::getUserId, userId)
                        .set(Notification::getIsRead, true));
    }
}
