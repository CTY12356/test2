package com.health.management.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.management.dto.UserDtos.UpdateProfileRequest;
import com.health.management.dto.UserDtos.UserProfileResponse;
import com.health.management.entity.User;
import com.health.management.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public UserProfileResponse getMe(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return toResponse(user);
    }

    public UserProfileResponse updateMe(Long userId, UpdateProfileRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        userMapper.updateById(user);
        return toResponse(user);
    }

    public List<UserProfileResponse> listAll() {
        return userMapper.selectList(new LambdaQueryWrapper<User>().orderByDesc(User::getCreatedAt))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public UserProfileResponse setStatus(Long userId, String status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
        return toResponse(user);
    }

    private UserProfileResponse toResponse(User user) {
        UserProfileResponse resp = new UserProfileResponse();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setAvatarUrl(user.getAvatarUrl());
        resp.setRole(user.getRole());
        resp.setStatus(user.getStatus());
        return resp;
    }
}
