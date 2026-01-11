package com.travel.dto.banner;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 轮播图响应
 */
@Data
public class BannerResponse {

    private List<BannerItem> list;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BannerItem {
        private Long id;
        private String imageUrl;
        private Long spotId;
        private String spotName;
        private Integer sortOrder;
    }
}
