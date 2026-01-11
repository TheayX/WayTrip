package com.travel.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单趋势响应
 */
@Data
public class OrderTrendResponse {

    private List<TrendItem> list;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendItem {
        private String date;
        private Long orderCount;
        private BigDecimal revenue;
    }
}
