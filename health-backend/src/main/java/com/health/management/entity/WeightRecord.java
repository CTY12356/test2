package com.health.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("weight_records")
public class WeightRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private BigDecimal weightKg;
    private LocalDate recordDate;
    private String remark;
    private LocalDateTime createdAt;
}
