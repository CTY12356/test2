package com.health.management.controller;

import com.health.management.common.ApiResponse;
import com.health.management.dto.RecordDtos.DietRecordRequest;
import com.health.management.entity.DietRecord;
import com.health.management.service.RecordService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/diet-records")
public class DietRecordController {
    private final RecordService recordService;

    public DietRecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping
    public ApiResponse<List<DietRecord>> list(Authentication authentication,
                                              @RequestParam(required = false) LocalDate date) {
        return ApiResponse.ok(recordService.listDietRecords(userId(authentication), date));
    }

    @PostMapping
    public ApiResponse<DietRecord> create(Authentication authentication,
                                         @Valid @RequestBody DietRecordRequest request) {
        return ApiResponse.ok(recordService.createDietRecord(userId(authentication), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<DietRecord> update(Authentication authentication, @PathVariable Long id,
                                          @Valid @RequestBody DietRecordRequest request) {
        return ApiResponse.ok(recordService.updateDietRecord(userId(authentication), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication authentication, @PathVariable Long id) {
        recordService.deleteDietRecord(userId(authentication), id);
        return ApiResponse.ok(null);
    }

    private Long userId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
