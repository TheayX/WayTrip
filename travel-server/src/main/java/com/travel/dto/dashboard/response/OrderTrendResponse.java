package com.travel.dto.dashboard.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单趋势响应对象。
 * <p>
 * 用于后台趋势图展示不同时间口径下的订单数量和金额变化。
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
