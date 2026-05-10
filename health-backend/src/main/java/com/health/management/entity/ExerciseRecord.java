package com.health.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("exercise_records")
public class ExerciseRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String exerciseType;
    private LocalDate recordDate;
    private Integer durationMinutes;
    private String intensity;
    private BigDecimal caloriesBurned;
    private String imageUrl;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
