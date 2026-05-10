package com.health.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("food_calories")
public class FoodCalorie {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String category;
    /** kcal 对应 unit_label 的参考热量（估算） */
    private BigDecimal caloriesPerUnit;
    private String unitLabel;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
