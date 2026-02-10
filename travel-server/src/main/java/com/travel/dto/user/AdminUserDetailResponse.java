package com.travel.dto.user;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端用户详情响应
 */
@Data
public class AdminUserDetailResponse {

    private Long id;
    private String nickname;
    private String avatar;
    private String phone;
    private String preferences;
    private LocalDateTime createdAt;

    // 统计数据
    private Integer orderCount;
    private Integer favoriteCount;
    private Integer ratingCount;

    // 最近订单
    private List<RecentOrder> recentOrders;

    @Data
    public static class RecentOrder {
        private Long id;
        private String orderNo;
        private String spotName;
        private String status;
        private LocalDateTime createdAt;
    }
}
