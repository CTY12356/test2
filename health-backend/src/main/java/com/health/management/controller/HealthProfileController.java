package com.health.management.controller;

import com.health.management.common.ApiResponse;
import com.health.management.dto.HealthDtos.HealthProfileRequest;
import com.health.management.dto.HealthDtos.WeightRecordRequest;
import com.health.management.entity.HealthProfile;
import com.health.management.entity.WeightRecord;
import com.health.management.service.HealthProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/profiles")
public class HealthProfileController {
    private final HealthProfileService healthProfileService;

    public HealthProfileController(HealthProfileService healthProfileService) {
        this.healthProfileService = healthProfileService;
    }

    @GetMapping("/me")
    public ApiResponse<HealthProfile> getProfile(Authentication authentication) {
        return ApiResponse.ok(healthProfileService.getProfile(userId(authentication)));
    }

    @PostMapping("/me")
    public ApiResponse<HealthProfile> saveProfile(Authentication authentication,
                                                  @Valid @RequestBody HealthProfileRequest request) {
        return ApiResponse.ok(healthProfileService.saveProfile(userId(authentication), request));
    }

    @PostMapping("/weights")
    public ApiResponse<WeightRecord> addWeight(Authentication authentication,
                                               @Valid @RequestBody WeightRecordRequest request) {
        return ApiResponse.ok(healthProfileService.addWeight(userId(authentication), request));
    }

    @GetMapping("/weights")
    public ApiResponse<List<WeightRecord>> listWeights(Authentication authentication) {
        return ApiResponse.ok(healthProfileService.listWeights(userId(authentication)));
    }

    private Long userId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
