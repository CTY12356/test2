package com.health.management.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

public class HealthDtos {
    @Data
    public static class HealthProfileRequest {
        @NotBlank
        private String gender;
        @NotNull
        @Min(1)
        private Integer age;
        @NotNull
        @DecimalMin("50")
        private BigDecimal heightCm;
        @NotNull
        @DecimalMin("10")
        private BigDecimal weightKg;
        private BigDecimal targetWeightKg;
        @NotBlank
        private String activityLevel;
        @NotBlank
        private String goal;
    }

    @Data
    public static class WeightRecordRequest {
        @NotNull
        @DecimalMin("10")
        private BigDecimal weightKg;
        @NotNull
        private LocalDate recordDate;
        private String remark;
    }

    @Data
    public static class DailySummaryResponse {
        private final LocalDate date;
        private final BigDecimal intakeCalories;
        private final BigDecimal bmrCalories;
        private final BigDecimal activityCalories;
        private final BigDecimal exerciseCalories;
        private final BigDecimal totalBurnedCalories;
        private final BigDecimal calorieBalance;
        private final String status;
    }

    @Data
    public static class FoodImageRecognizeRequest {
        @NotBlank
        private String imageUrl;
    }

    /** 云 API 识别结果（估算值，须由用户确认后再保存饮食记录） */
    @Data
    public static class FoodRecognitionSuggestion {
        private String name;
        private BigDecimal estimatedTotalKcal;
        private String basis;
        private String note;
    }
}
