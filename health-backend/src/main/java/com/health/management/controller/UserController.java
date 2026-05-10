package com.health.management.controller;

import com.health.management.common.ApiResponse;
import com.health.management.dto.UserDtos.UpdateProfileRequest;
import com.health.management.dto.UserDtos.UserProfileResponse;
import com.health.management.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMe(Authentication authentication) {
        return ApiResponse.ok(userService.getMe(userId(authentication)));
    }

    @PutMapping("/me")
    public ApiResponse<UserProfileResponse> updateMe(Authentication authentication,
                                                     @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(userService.updateMe(userId(authentication), request));
    }

    private Long userId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
