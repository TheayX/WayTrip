package com.travel.controller.admin;

import com.travel.common.result.ApiResponse;
import com.travel.dto.recommendation.config.RecommendationConfigBundleDTO;
import com.travel.dto.recommendation.response.RecommendationResponse;
import com.travel.dto.recommendation.response.RecommendationStatusDTO;
import com.travel.dto.recommendation.response.SimilarityPreviewResponse;
import com.travel.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端推荐控制器，负责推荐配置、状态查看与调试接口。
 * <p>
 * 推荐系统的运维、调试和配置能力只开放给后台，避免影响用户端稳定接口形态。
 */
@Tag(name = "管理端-推荐系统", description = "管理端推荐系统控制接口")
@RestController
@RequestMapping("/api/admin/v1/recommendation")
@RequiredArgsConstructor
@Slf4j
public class AdminRecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "手动更新物品相似度矩阵")
    @PostMapping("/update-matrix")
    public ApiResponse<Void> updateSimilarityMatrix() {
        log.info("管理端触发相似度矩阵更新：开始执行");
        recommendationService.updateSimilarityMatrix();
        log.info("管理端触发相似度矩阵更新：执行完成");
        return ApiResponse.success(null);
    }

    @Operation(summary = "获取推荐算法配置")
    @GetMapping("/config")
    public ApiResponse<RecommendationConfigBundleDTO> getConfig() {
        return ApiResponse.success(recommendationService.getConfig());
    }

    @Operation(summary = "更新推荐算法配置")
    @PutMapping("/config")
    public ApiResponse<Void> updateConfig(@RequestBody RecommendationConfigBundleDTO config) {
        // 配置修改统一走服务层校验，控制器只负责承接后台调参请求。
        recommendationService.updateConfig(config);
        return ApiResponse.success(null);
    }

    @Operation(summary = "获取推荐引擎运行状态")
    @GetMapping("/status")
    public ApiResponse<RecommendationStatusDTO> getStatus() {
        return ApiResponse.success(recommendationService.getStatus());
    }

    /**
     * 仅供管理端调试使用，可优先查看缓存结果或预览最新结果，并按需决定是否写入缓存。
     */
    @Operation(summary = "调试预览指定用户的推荐结果")
    @GetMapping("/preview")
    public ApiResponse<RecommendationResponse> previewRecommendations(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(defaultValue = "cache") String mode,
            @RequestParam(defaultValue = "false") Boolean writeCache,
            @RequestParam(defaultValue = "false") Boolean rotate,
            @RequestParam(defaultValue = "false") Boolean debug) {
        log.info(
            "管理端请求推荐调试预览：用户ID={}，返回条数={}，预览模式={}，是否写入缓存={}，是否轮换调试={}，是否输出详细调试日志={}",
            userId,
            limit,
            mode,
            writeCache,
            rotate,
            debug
        );
        return ApiResponse.success(recommendationService.previewRecommendations(userId, limit, mode, writeCache, rotate, debug));
    }

    /**
     * 用于核对相似度矩阵结果，返回指定景点的相似邻居列表。
     */
    @Operation(summary = "预览指定景点的相似邻居")
    @GetMapping("/similarity-preview")
    public ApiResponse<SimilarityPreviewResponse> previewSimilarityNeighbors(
            @RequestParam Long spotId,
            @RequestParam(defaultValue = "10") Integer limit) {
        log.info("管理端请求相似邻居预览：景点ID={}，返回条数={}", spotId, limit);
        return ApiResponse.success(recommendationService.previewSimilarityNeighbors(spotId, limit));
    }
}
