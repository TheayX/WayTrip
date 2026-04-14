package com.travel.service.ai.tool;

import com.travel.common.result.PageResult;
import com.travel.dto.guide.request.GuideListRequest;
import com.travel.dto.guide.response.GuideListResponse;
import com.travel.dto.spot.request.SpotListRequest;
import com.travel.dto.spot.response.SpotDetailResponse;
import com.travel.dto.spot.response.SpotListResponse;
import com.travel.service.GuideService;
import com.travel.service.SpotService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 景点与攻略工具集合。
 */
@Component
@RequiredArgsConstructor
public class SpotAiTools {

    private final SpotService spotService;
    private final GuideService guideService;
    private final AiToolContextHolder aiToolContextHolder;

    /**
     * 获取景点详情。
     *
     * @param spotId 景点 ID
     * @return 景点详情摘要
     */
    @Tool(description = "获取指定景点的详情信息")
    public Map<String, Object> getSpotDetails(
            @ToolParam(description = "景点 ID", required = true) Long spotId) {
        SpotDetailResponse detail = spotService.getSpotDetail(spotId, aiToolContextHolder.getCurrentUserId());
        aiToolContextHolder.addToolTrace(
                "getSpotDetails",
                "spot",
                true,
                "已查询景点详情，景点名称为 " + detail.getName()
        );
        return simplifySpotDetail(detail);
    }

    /**
     * 搜索景点。
     *
     * @param keyword 关键词
     * @param categoryId 分类 ID
     * @param regionId 地区 ID
     * @param limit 返回条数
     * @return 景点列表摘要
     */
    @Tool(description = "按关键词、分类和地区搜索景点")
    public Map<String, Object> searchSpots(
            @ToolParam(description = "搜索关键词", required = false) String keyword,
            @ToolParam(description = "分类 ID", required = false) Long categoryId,
            @ToolParam(description = "地区 ID", required = false) Long regionId,
            @ToolParam(description = "返回条数，建议 3 到 10 之间", required = false) Integer limit) {
        if (keyword != null && !keyword.isBlank()) {
            PageResult<SpotListResponse> response = spotService.searchSpots(keyword.trim(), 1, normalizeLimit(limit, 5));
            aiToolContextHolder.addToolTrace(
                    "searchSpots",
                    "spot",
                    true,
                    "已按关键词搜索景点，共 " + response.getTotal() + " 条"
            );
            return Map.of("total", response.getTotal(), "list", simplifySpotList(response.getList()));
        }
        SpotListRequest request = new SpotListRequest();
        request.setPage(1);
        request.setPageSize(normalizeLimit(limit, 5));
        request.setCategoryId(categoryId);
        request.setRegionId(regionId);
        PageResult<SpotListResponse> response = spotService.getSpotList(request);
        aiToolContextHolder.addToolTrace(
                "searchSpots",
                "spot",
                true,
                "已按分类或地区筛选景点，共 " + response.getTotal() + " 条"
        );
        return Map.of("total", response.getTotal(), "list", simplifySpotList(response.getList()));
    }

    /**
     * 获取与景点相关的攻略摘要。
     *
     * @param keyword 搜索关键词，通常是景点名称
     * @param limit 返回条数
     * @return 攻略摘要列表
     */
    @Tool(description = "根据关键词获取相关攻略摘要")
    public Map<String, Object> getGuideSummariesByKeyword(
            @ToolParam(description = "搜索关键词，通常为景点名称或玩法关键词", required = true) String keyword,
            @ToolParam(description = "返回条数，建议 3 到 8 之间", required = false) Integer limit) {
        GuideListRequest request = new GuideListRequest();
        request.setKeyword(keyword);
        request.setPage(1);
        request.setPageSize(normalizeLimit(limit, 5));
        PageResult<GuideListResponse> response = guideService.getGuideList(request);
        aiToolContextHolder.addToolTrace(
                "getGuideSummariesByKeyword",
                "guide",
                true,
                "已查询相关攻略摘要，共 " + response.getTotal() + " 条"
        );
        return Map.of("total", response.getTotal(), "list", simplifyGuideList(response.getList()));
    }

    private int normalizeLimit(Integer limit, int fallback) {
        if (limit == null || limit <= 0) {
            return fallback;
        }
        return Math.min(limit, 10);
    }

    private Map<String, Object> simplifySpotDetail(SpotDetailResponse detail) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", detail.getId());
        row.put("name", detail.getName());
        row.put("price", detail.getPrice());
        row.put("openTime", detail.getOpenTime());
        row.put("address", detail.getAddress());
        row.put("avgRating", detail.getAvgRating());
        row.put("ratingCount", detail.getRatingCount());
        row.put("regionName", detail.getRegionName());
        row.put("categoryName", detail.getCategoryName());
        row.put("description", detail.getDescription());
        return row;
    }

    private List<Map<String, Object>> simplifySpotList(List<SpotListResponse> list) {
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
            return row;
        }).toList();
    }

    private List<Map<String, Object>> simplifyGuideList(List<GuideListResponse> list) {
        if (list == null) {
            return List.of();
        }
        return list.stream().map(item -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", item.getId());
            row.put("title", item.getTitle());
            row.put("category", item.getCategory());
            row.put("viewCount", item.getViewCount());
            row.put("createdAt", item.getCreatedAt());
            return row;
        }).toList();
    }
}
