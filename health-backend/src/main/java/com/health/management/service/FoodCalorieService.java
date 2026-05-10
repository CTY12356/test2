package com.health.management.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.management.entity.FoodCalorie;
import com.health.management.mapper.FoodCalorieMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FoodCalorieService {
    private static final int DEFAULT_LIMIT = 30;
    private static final int MAX_LIMIT = 50;

    private final FoodCalorieMapper foodCalorieMapper;

    public FoodCalorieService(FoodCalorieMapper foodCalorieMapper) {
        this.foodCalorieMapper = foodCalorieMapper;
    }

    /**
     * 按名称关键词模糊搜索常见食物参考热量，空关键词返回空列表。
     */
    public List<FoodCalorie> searchByNameKeyword(String keyword, Integer limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String kw = keyword.trim();
        int lim = limit == null ? DEFAULT_LIMIT : limit;
        lim = Math.max(1, Math.min(lim, MAX_LIMIT));
        return foodCalorieMapper.selectList(
                new LambdaQueryWrapper<FoodCalorie>()
                        .like(FoodCalorie::getName, kw)
                        .orderByAsc(FoodCalorie::getSortOrder)
                        .orderByAsc(FoodCalorie::getName)
                        .last("LIMIT " + lim));
    }
}
