package com.health.management.common;

import java.util.Set;

/**
 * 运动类型（与前端 constants/options 中 EXERCISE_TYPES 的 value 保持一致）
 */
public final class ExerciseConstants {
    public static final Set<String> ALLOWED_EXERCISE_TYPES = Set.of(
            "跑步",
            "快走",
            "骑行",
            "游泳",
            "跳绳",
            "力量训练",
            "瑜伽"
    );

    private ExerciseConstants() {
    }
}
