package com.health.management.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.management.dto.AuthDtos.AuthResponse;
import com.health.management.dto.AuthDtos.LoginRequest;
import com.health.management.dto.AuthDtos.RegisterRequest;
import com.health.management.entity.User;
import com.health.management.mapper.UserMapper;
import com.health.management.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (count > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setNickname(request.getNickname() != null && !request.getNickname().isBlank()
                ? request.getNickname() : request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");
        user.setStatus("ACTIVE");
        userMapper.insert(user);
        String token = jwtService.createToken(user.getId(), user.getUsername(), user.getRole());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getNickname(), user.getRole(), user.getAvatarUrl());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if ("BANNED".equals(user.getStatus())) {
            throw new IllegalArgumentException("账号已被禁用，请联系管理员");
        }
        String token = jwtService.createToken(user.getId(), user.getUsername(), user.getRole());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getNickname(), user.getRole(), user.getAvatarUrl());
    }
}
