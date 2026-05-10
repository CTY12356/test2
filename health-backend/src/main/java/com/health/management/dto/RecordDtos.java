package com.health.management.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RecordDtos {
    @Data
    public static class ExerciseRecordRequest {
        @NotBlank
        private String exerciseType;
        @NotNull
        private LocalDate recordDate;
        @NotNull
        @Min(1)
        private Integer durationMinutes;
        private String intensity;
        @NotNull
        @DecimalMin("0")
        private BigDecimal caloriesBurned;
        private String imageUrl;
        private String remark;
    }

    @Data
    public static class DietRecordRequest {
        @NotNull
        private LocalDate recordDate;
        @NotBlank
        private String mealType;
        @NotBlank
        private String foodName;
        private String amount;
        @NotNull
        @DecimalMin("0")
        private BigDecimal calories;
        private String imageUrl;
        private String remark;
    }
}
