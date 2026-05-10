package com.health.management.common;

import java.util.Set;

/**
 * 论坛帖子分类（与前端 constants/options 中 FORUM_CATEGORIES 的 value 保持一致）
 */
public final class ForumConstants {
    public static final Set<String> ALLOWED_POST_CATEGORIES = Set.of(
            "健康指南",
            "减肥打卡",
            "运动计划",
            "饮食搭配",
            "问答求助",
            "成功案例",
            "其他"
    );

    private ForumConstants() {
    }
}
