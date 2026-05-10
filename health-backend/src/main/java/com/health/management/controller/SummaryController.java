package com.health.management.controller;

import com.health.management.common.ApiResponse;
import com.health.management.dto.HealthDtos.DailySummaryResponse;
import com.health.management.service.RecordService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/summary")
public class SummaryController {
    private final RecordService recordService;

    public SummaryController(RecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping("/daily")
    public ApiResponse<DailySummaryResponse> daily(Authentication authentication,
                                                   @RequestParam(required = false) LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();
        return ApiResponse.ok(recordService.getDailySummary(userId(authentication), target));
    }

    @GetMapping("/trend")
    public ApiResponse<List<DailySummaryResponse>> trend(Authentication authentication) {
        return ApiResponse.ok(recordService.getWeeklyTrend(userId(authentication)));
    }

    private Long userId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
