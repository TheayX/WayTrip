package com.travel.service.ai.tool;

import com.travel.dto.home.response.HotSpotResponse;
import com.travel.dto.home.response.NearbySpotResponse;
import com.travel.dto.recommendation.response.RecommendationResponse;
import com.travel.dto.recommendation.response.SimilarityPreviewResponse;
import com.travel.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 推荐工具集合。
 */
@Component
@RequiredArgsConstructor
public class RecommendationAiTools {

    private final RecommendationService recommendationService;
    private final AiToolContextHolder aiToolContextHolder;

    /**
     * 获取当前登录用户的个性化推荐。
     *
     * @param limit 返回条数
     * @return 推荐结果摘要
     */
    @Tool(description = "获取当前登录用户的个性化景点推荐列表")
    public Map<String, Object> getPersonalizedRecommendations(
            @ToolParam(description = "返回条数，建议 3 到 10 之间", required = false) Integer limit) {
        RecommendationResponse response =
                recommendationService.getRecommendations(aiToolContextHolder.requireCurrentUserId(), normalizeLimit(limit, 5));
        aiToolContextHolder.addToolTrace(
                "getPersonalizedRecommendations",
                "recommendation",
                true,
                "已查询当前登录用户的个性化推荐结果，共 " + (response.getList() == null ? 0 : response.getList().size()) + " 条"
        );
        return AiToolResponse.success(
                "已获取个性化推荐结果",
                Map.of(
                        "type", response.getType(),
                        "needPreference", Boolean.TRUE.equals(response.getNeedPreference()),
                        "spots", simplifyRecommendationItems(response.getList())
                )
        );
    }

    /**
     * 获取热门推荐景点。
     *
     * @param limit 返回条数
     * @return 热门景点摘要
     */
    @Tool(description = "获取平台当前热门景点列表")
    public Map<String, Object> getHotSpotRecommendations(
            @ToolParam(description = "返回条数，建议 3 到 10 之间", required = false) Integer limit) {
        HotSpotResponse response = recommendationService.getHotSpots(normalizeLimit(limit, 5));
        aiToolContextHolder.addToolTrace(
                "getHotSpotRecommendations",
                "recommendation",
                true,
                "已查询热门景点，共 " + (response.getList() == null ? 0 : response.getList().size()) + " 条"
        );
        return AiToolResponse.success(
                "已获取热门景点推荐",
                Map.of("spots", response.getList())
        );
    }

    /**
     * 获取附近景点。
     *
     * @param latitude 纬度
     * @param longitude 经度
     * @param limit 返回条数
     * @return 附近景点摘要
     */
    @Tool(description = "根据经纬度获取附近景点")
    public Map<String, Object> getNearbySpots(
            @ToolParam(description = "纬度", required = true) Double latitude,
            @ToolParam(description = "经度", required = true) Double longitude,
            @ToolParam(description = "返回条数，建议 3 到 8 之间", required = false) Integer limit) {
        NearbySpotResponse response = recommendationService.getNearbySpots(
                BigDecimal.valueOf(latitude),
                BigDecimal.valueOf(longitude),
                normalizeLimit(limit, 3)
        );
        aiToolContextHolder.addToolTrace(
                "getNearbySpots",
                "recommendation",
                true,
                "已按经纬度查询附近景点，共 " + response.getTotal() + " 条"
        );
        return AiToolResponse.success(
                "已获取附近景点",
                Map.of(
                        "total", response.getTotal(),
                        "nearestDistanceKm", response.getNearestDistanceKm(),
                        "spots", response.getList()
                )
        );
    }

    /**
     * 获取相似景点。
     *
     * @param spotId 景点 ID
     * @param limit 返回条数
     * @return 相似景点摘要
     */
    @Tool(description = "根据指定景点获取相似景点列表")
    public Map<String, Object> getSimilarSpots(
            @ToolParam(description = "景点 ID", required = true) Long spotId,
            @ToolParam(description = "返回条数，建议 3 到 8 之间", required = false) Integer limit) {
        SimilarityPreviewResponse response = recommendationService.previewSimilarityNeighbors(spotId, normalizeLimit(limit, 5));
        aiToolContextHolder.addToolTrace(
                "getSimilarSpots",
                "recommendation",
                true,
                "已查询景点 " + spotId + " 的相似景点，共 " + (response.getNeighbors() == null ? 0 : response.getNeighbors().size()) + " 条"
        );
        return AiToolResponse.success(
                "已获取相似景点",
                Map.of(
                        "spotId", response.getSpotId(),
                        "spotName", response.getSpotName(),
                        "neighbors", response.getNeighbors()
                )
        );
    }

    private int normalizeLimit(Integer limit, int fallback) {
        if (limit == null || limit <= 0) {
            return fallback;
        }
        return Math.min(limit, 10);
    }

    private List<Map<String, Object>> simplifyRecommendationItems(List<RecommendationResponse.SpotItem> list) {
        if (list == null) {
            return List.of();
        }
        return list.stream().map(item -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", item.getId());
            row.put("name", item.getName());
            row.put("price", item.getPrice());
            row.put("avgRating", item.getAvgRating());
            row.put("categoryName", item.getCategoryName());
            row.put("regionName", item.getRegionName());
            row.put("score", item.getScore());
            return row;
        }).toList();
    }
}
