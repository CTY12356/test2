package com.health.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("diet_records")
public class DietRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate recordDate;
    private String mealType;
    private String foodName;
    private String amount;
    private BigDecimal calories;
    private String imageUrl;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
