package com.travel.service.ai.tool;

import com.travel.dto.user.response.AdminUserDetailResponse;
import com.travel.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 用户画像工具集合。
 */
@Component
@RequiredArgsConstructor
public class UserProfileAiTools {

    /**
     * 用户画像查询服务。
     */
    private final UserProfileService userProfileService;

    /**
     * AI 工具上下文。
     */
    private final AiToolContextHolder aiToolContextHolder;

    /**
     * 获取当前登录用户的行为画像摘要。
     *
     * @return 行为画像摘要
     */
    @Tool(description = "获取当前登录用户的偏好、浏览、收藏、订单和评价行为摘要")
    public Map<String, Object> getCurrentUserProfileSummary() {
        Long userId = aiToolContextHolder.requireCurrentUserId();
        AdminUserDetailResponse detail = userProfileService.getAdminUserDetail(userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", detail.getId());
        result.put("nickname", detail.getNickname());
        result.put("orderCount", detail.getOrderCount());
        result.put("favoriteCount", detail.getFavoriteCount());
        result.put("ratingCount", detail.getRatingCount());
        result.put("viewCount", detail.getViewCount());
        result.put("preferenceSummary", detail.getPreferenceSummary());
        result.put("favoriteSummary", detail.getFavoriteSummary());
        result.put("viewSummary", detail.getViewSummary());
        result.put("recentOrders", detail.getRecentOrders());
        aiToolContextHolder.addToolTrace(
                "getCurrentUserProfileSummary",
                "user-profile",
                true,
                "已获取当前用户行为画像摘要"
        );
        return AiToolResponse.success("已获取当前用户行为画像摘要", result);
    }
}
