package com.travel.dto.banner;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端轮播图列表响应
 */
@Data
public class AdminBannerListResponse {

    private List<BannerItem> list;
    private Long total;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BannerItem {
        private Long id;
        private String imageUrl;
        private Long spotId;
        private String spotName;
        private Integer sortOrder;
        private Integer enabled;
        private LocalDateTime createdAt;
    }
}
