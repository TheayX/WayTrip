package com.travel.dto.home;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 附近景点响应对象。
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
