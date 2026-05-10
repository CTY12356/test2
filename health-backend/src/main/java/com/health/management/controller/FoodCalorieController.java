package com.health.management.controller;

import com.health.management.common.ApiResponse;
import com.health.management.dto.HealthDtos.FoodImageRecognizeRequest;
import com.health.management.dto.HealthDtos.FoodRecognitionSuggestion;
import com.health.management.entity.FoodCalorie;
import com.health.management.service.FoodCalorieService;
import com.health.management.service.FoodVisionCloudService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/food-calories")
@Tag(name = "食物热量库", description = "常见食物参考热量检索；可选云图识别（需登录）")
public class FoodCalorieController {
    private final FoodCalorieService foodCalorieService;
    private final FoodVisionCloudService foodVisionCloudService;

    public FoodCalorieController(FoodCalorieService foodCalorieService,
                                 FoodVisionCloudService foodVisionCloudService) {
        this.foodCalorieService = foodCalorieService;
        this.foodVisionCloudService = foodVisionCloudService;
    }

    @GetMapping("/search")
    @Operation(summary = "按关键词搜索", description = "名称模糊匹配，返回参考热量及计量基准（每份/每100g 等），为空 keyword 返回空列表")
    public ApiResponse<List<FoodCalorie>> search(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(foodCalorieService.searchByNameKeyword(keyword, limit));
    }

    @PostMapping("/recognize")
    @Operation(summary = "餐图识别（云 API）", description = "传入本站已上传图片 URL，调用配置的 OpenAI 兼容多模态接口（如豆包方舟 ep- 接入点、OpenAI）返回估算结果；须用户确认后再保存记录")
    public ApiResponse<List<FoodRecognitionSuggestion>> recognize(@Valid @RequestBody FoodImageRecognizeRequest request) {
        try {
            return ApiResponse.ok(foodVisionCloudService.recognize(request.getImageUrl()));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("识别失败：" + e.getMessage());
        }
    }
}
