package com.health.management.controller;

import com.health.management.common.ApiResponse;
import com.health.management.dto.RecordDtos.ExerciseRecordRequest;
import com.health.management.entity.ExerciseRecord;
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
@RequestMapping("/exercise-records")
public class ExerciseRecordController {
    private final RecordService recordService;

    public ExerciseRecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping
    public ApiResponse<List<ExerciseRecord>> list(Authentication authentication,
                                                  @RequestParam(required = false) LocalDate date) {
        return ApiResponse.ok(recordService.listExerciseRecords(userId(authentication), date));
    }

    @PostMapping
    public ApiResponse<ExerciseRecord> create(Authentication authentication,
                                              @Valid @RequestBody ExerciseRecordRequest request) {
        return ApiResponse.ok(recordService.createExerciseRecord(userId(authentication), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ExerciseRecord> update(Authentication authentication, @PathVariable Long id,
                                              @Valid @RequestBody ExerciseRecordRequest request) {
        return ApiResponse.ok(recordService.updateExerciseRecord(userId(authentication), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication authentication, @PathVariable Long id) {
        recordService.deleteExerciseRecord(userId(authentication), id);
        return ApiResponse.ok(null);
    }

    private Long userId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
