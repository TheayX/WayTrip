package com.travel.controller.admin;

import com.travel.common.result.ApiResponse;
import com.travel.dto.recommendation.RecommendationConfigDTO;
import com.travel.dto.recommendation.RecommendationResponse;
import com.travel.dto.recommendation.RecommendationStatusDTO;
import com.travel.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端-推荐系统", description = "管理端推荐系统控制接口")
@RestController
@RequestMapping("/api/admin/v1/recommendation")
@RequiredArgsConstructor
public class AdminRecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "手动更新物品相似度矩阵")
    @PostMapping("/update-matrix")
    public ApiResponse<Void> updateSimilarityMatrix() {
        recommendationService.updateSimilarityMatrix();
        return ApiResponse.success(null);
    }

    @Operation(summary = "获取推荐算法配置")
    @GetMapping("/config")
    public ApiResponse<RecommendationConfigDTO> getConfig() {
        return ApiResponse.success(recommendationService.getConfig());
    }

    @Operation(summary = "更新推荐算法配置")
    @PutMapping("/config")
    public ApiResponse<Void> updateConfig(@RequestBody RecommendationConfigDTO config) {
        recommendationService.updateConfig(config);
        return ApiResponse.success(null);
    }

    @Operation(summary = "获取推荐引擎运行状态")
    @GetMapping("/status")
    public ApiResponse<RecommendationStatusDTO> getStatus() {
        return ApiResponse.success(recommendationService.getStatus());
    }

    @Operation(summary = "调试预览指定用户的推荐结果")
    @GetMapping("/preview")
    public ApiResponse<RecommendationResponse> previewRecommendations(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(defaultValue = "false") Boolean refresh,
            @RequestParam(defaultValue = "false") Boolean debug) {
        return ApiResponse.success(recommendationService.previewRecommendations(userId, limit, refresh, debug));
    }
}
