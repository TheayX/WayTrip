package com.travel.dto.dashboard.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 订单热力图响应对象。
 * <p>
 * 面向后台热力图组件，返回按日期聚合后的订单分布结果。
 */
@Data
public class OrderHeatmapResponse {

    private Integer year;
    private List<HeatmapItem> list;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeatmapItem {
        private String date;
        private Long orderCount;
    }
}
