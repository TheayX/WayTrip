package com.travel.dto.user.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端用户详情响应对象。
 */
@Data
public class AdminUserDetailResponse {

    private Long id;
    private String nickname;
    private String avatar;
    private String phone;
    private String preferences;
    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 统计数据
    private Integer orderCount;
    private Integer favoriteCount;
    private Integer ratingCount;
    private Integer viewCount;

    // 行为摘要
    private PreferenceSummary preferenceSummary;
    private FavoriteSummary favoriteSummary;
    private ViewSummary viewSummary;

    // 最近订单
    private List<RecentOrder> recentOrders;

    @Data
    public static class RecentOrder {
        private Long id;
        private String orderNo;
        private String spotName;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class PreferenceSummary {
        private Integer count;
        private List<String> tags;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class FavoriteSummary {
        private String latestSpotName;
        private LocalDateTime latestCreatedAt;
    }

    @Data
    public static class ViewSummary {
        private String latestSpotName;
        private LocalDateTime latestCreatedAt;
        private String topSource;
        private Integer averageDuration;
    }
}
