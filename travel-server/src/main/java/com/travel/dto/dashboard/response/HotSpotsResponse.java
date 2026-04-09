package com.travel.dto.dashboard.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 热门景点统计响应对象。
 * <p>
 * 用于后台仪表板展示当前热度最高的景点及其辅助统计信息。
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
