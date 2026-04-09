package com.travel.dto.home.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 附近景点响应对象。
 * <p>
 * 面向首页附近探索模块，除景点信息外还保留距离等位置相关展示字段。
 */
@Data
public class NearbySpotResponse {

    private List<SpotItem> list;

    private Integer total;

    private BigDecimal nearestDistanceKm;

    @Data
    public static class SpotItem {
        private Long id;
        private String name;
        private String coverImage;
        private BigDecimal price;
        private BigDecimal avgRating;
        private String categoryName;
        private String regionName;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private BigDecimal distanceKm;
    }
}
