package com.travel.service.ai.chat.operation;

import com.travel.dto.dashboard.response.HotSpotsResponse;
import com.travel.dto.dashboard.response.OrderTrendResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 运营分析 AI 回复生成器。
 */
@Component
public class OperationAiResponseComposer {

    /**
     * 生成运营分析回复。
     *
     * @param toolResult 工具执行结果
     * @return 用户可见回复
     */
    public String compose(OperationAiToolResult toolResult) {
        return switch (toolResult.intent()) {
            case OPERATION_OVERVIEW -> formatOverview(toolResult.payload());
            case ORDER_TREND -> formatOrderTrend(toolResult.payload());
            case HOT_SPOTS -> formatHotSpots(toolResult.payload());
            default -> "暂时无法确认这个运营问题，请换个问法或到管理端仪表板查看。";
        };
    }

    private String formatOverview(Map<String, Object> payload) {
        StringBuilder reply = new StringBuilder("当前运营概览：");
        reply.append("\n- 总用户：").append(Objects.toString(payload.get("totalUsers"), "0"));
        reply.append("，上架景点：").append(Objects.toString(payload.get("totalSpots"), "0"));
        reply.append("，有效订单：").append(Objects.toString(payload.get("totalOrders"), "0"));
        reply.append("，累计营收：").append(Objects.toString(payload.get("totalRevenue"), "0"));
        reply.append("\n- 今日订单：").append(Objects.toString(payload.get("todayOrders"), "0"));
        reply.append("，今日营收：").append(Objects.toString(payload.get("todayRevenue"), "0"));
        reply.append("，今日新增用户：").append(Objects.toString(payload.get("todayNewUsers"), "0"));
        reply.append("，今日新增景点：").append(Objects.toString(payload.get("todayNewSpots"), "0"));
        reply.append("\n- 昨日订单：").append(Objects.toString(payload.get("yesterdayOrders"), "0"));
        reply.append("，昨日营收：").append(Objects.toString(payload.get("yesterdayRevenue"), "0"));
        reply.append("\n这些结论来自管理端仪表盘统计，具体明细请以后台页面为准。");
        return reply.toString();
    }

    private String formatOrderTrend(Map<String, Object> payload) {
        Object listValue = payload.get("list");
        if (!(listValue instanceof List<?> list) || list.isEmpty()) {
            return "当前没有查询到订单趋势数据，请确认统计区间内是否有订单。";
        }
        long totalOrders = 0L;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        String bestDate = "";
        long bestOrders = -1L;
        for (Object item : list) {
            if (item instanceof OrderTrendResponse.TrendItem trendItem) {
                long count = trendItem.getOrderCount() == null ? 0L : trendItem.getOrderCount();
                totalOrders += count;
                totalRevenue = totalRevenue.add(trendItem.getRevenue() == null ? BigDecimal.ZERO : trendItem.getRevenue());
                if (count > bestOrders) {
                    bestOrders = count;
                    bestDate = trendItem.getDate();
                }
            }
        }
        return "最近 " + Objects.toString(payload.get("days"), "7") + " 天订单趋势："
                + "\n- 订单合计：" + totalOrders
                + "\n- 营收合计：" + totalRevenue
                + "\n- 订单最高日期：" + (bestDate == null || bestDate.isBlank() ? "暂无" : bestDate)
                + "，订单数：" + Math.max(bestOrders, 0L)
                + "\n趋势结论只基于当前统计接口返回的数据，转化率等指标需要等对应统计接口补齐后再分析。";
    }

    private String formatHotSpots(Map<String, Object> payload) {
        Object listValue = payload.get("list");
        if (!(listValue instanceof List<?> list) || list.isEmpty()) {
            return "当前没有查询到热门景点统计，请确认是否已有有效订单数据。";
        }
        StringBuilder reply = new StringBuilder("当前热门景点：");
        for (Object item : list) {
            if (item instanceof HotSpotsResponse.SpotItem spot) {
                reply.append("\n- ")
                        .append(Objects.toString(spot.getName(), "未知景点"))
                        .append("，订单：")
                        .append(Objects.toString(spot.getOrderCount(), "0"))
                        .append("，营收：")
                        .append(Objects.toString(spot.getRevenue(), "0"))
                        .append("，评分：")
                        .append(Objects.toString(spot.getAvgRating(), "暂无"));
            }
        }
        reply.append("\n以上按管理端热门景点统计生成，热度口径以后台接口为准。");
        return reply.toString();
    }
}
