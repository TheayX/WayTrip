package com.travel.controller.admin;

import com.travel.common.result.ApiResponse;
import com.travel.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-推荐系统", description = "管理端推荐系统控制接口")
@RestController
@RequestMapping("/api/admin/v1/recommendation")
@RequiredArgsConstructor
public class AdminRecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "手动更新物品相似度矩阵")
    @PostMapping("/update-matrix")
    public ApiResponse<Void> updateSimilarityMatrix() {
        // 直接在当前请求线程中调用，前端会等待响应，直到计算完成
        recommendationService.updateSimilarityMatrix();
        return ApiResponse.success(null);
    }
}
