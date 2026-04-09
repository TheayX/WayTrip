package com.travel.dto.home.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 首页热门景点响应对象。
 * <p>
 * 面向首页热门模块，返回按热度排序的景点列表与基础展示字段。
 */
@Data
public class HotSpotResponse {

    private List<SpotItem> list;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpotItem {
        private Long id;
        private String name;
        private String coverImage;
        private BigDecimal price;
        private BigDecimal avgRating;
        private Integer heatScore;
        private String categoryName;
    }
}
