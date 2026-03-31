package com.travel.dto.banner.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户端轮播图响应对象。
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
