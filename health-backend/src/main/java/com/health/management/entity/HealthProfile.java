package com.health.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("health_profiles")
public class HealthProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String gender;
    private Integer age;
    private BigDecimal heightCm;
    private BigDecimal weightKg;
    private BigDecimal targetWeightKg;
    private String activityLevel;
    private String goal;
    private BigDecimal bmi;
    private BigDecimal bmr;
    private BigDecimal recommendedCalories;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
