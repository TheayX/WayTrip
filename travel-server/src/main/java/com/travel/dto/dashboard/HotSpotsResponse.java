package com.travel.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 热门景点响应
 */
@Data
public class HotSpotsResponse {

    private List<SpotItem> list;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpotItem {
        private Long id;
        private String name;
        private Long orderCount;
        private BigDecimal revenue;
        private BigDecimal avgRating;
    }
}
