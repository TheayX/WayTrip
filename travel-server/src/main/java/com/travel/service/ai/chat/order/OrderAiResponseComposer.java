package com.travel.service.ai.chat.order;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 订单 AI 回复生成器，负责把真实工具结果组织成用户可读回复。
 */
@Component
public class OrderAiResponseComposer {

    /**
     * 生成订单意图的最终回复。
     *
     * @param toolResult 工具执行结果
     * @return 用户可见回复
     */
    public String compose(OrderAiToolResult toolResult) {
        return switch (toolResult.intent()) {
            case GUIDE_STATUS, GUIDE_REFUND, GUIDE_PAGE -> formatOrderGuideReply(toolResult.payload());
            case LIST_ORDERS -> formatOrderListReply(toolResult.payload());
            case REFUND_ELIGIBILITY -> formatRefundEligibilityReply(toolResult.payload());
            case DETAIL_BY_ORDER_NO -> formatOrderDetailReply(toolResult.payload());
            default -> "暂时无法确认这个订单问题，请换个问法或稍后重试。";
        };
    }

    private String formatOrderGuideReply(Map<String, Object> guide) {
        if (guide == null || guide.isEmpty()) {
            return "暂时无法获取订单说明，请稍后重试。";
        }
        String title = Objects.toString(guide.get("title"), "订单说明");
        Object content = guide.get("content");
        if (!(content instanceof List<?> items) || items.isEmpty()) {
            return title;
        }
        String body = items.stream()
                .map(String::valueOf)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));
        return title + "：\n" + body;
    }

    private String formatOrderListReply(Map<String, Object> orders) {
        long total = parseLong(orders.get("total"));
        Object listValue = orders.get("list");
        if (!(listValue instanceof List<?> list) || list.isEmpty()) {
            return "没有查询到符合条件的订单。你可以到“我的订单”页面确认是否使用了其他账号登录。";
        }
        StringBuilder reply = new StringBuilder("已查询到你的订单，共 ").append(total).append(" 条。");
        reply.append("\n最近订单：");
        for (Object item : list) {
            if (item instanceof Map<?, ?> row) {
                reply.append("\n- ")
                        .append(Objects.toString(row.get("spotName"), "未命名景点"))
                        .append("，状态：")
                        .append(Objects.toString(row.get("statusText"), "未知"))
                        .append("，订单号：")
                        .append(Objects.toString(row.get("orderNo"), "无"))
                        .append("，出行日期：")
                        .append(Objects.toString(row.get("visitDate"), "未设置"))
                        .append("，金额：")
                        .append(Objects.toString(row.get("totalPrice"), "未确认"));
            }
        }
        if (total > list.size()) {
            reply.append("\n当前先展示最近 ").append(list.size()).append(" 条，更多订单请在“我的订单”页面继续查看。");
        }
        return reply.toString();
    }

    private String formatRefundEligibilityReply(Map<String, Object> detail) {
        if (Boolean.FALSE.equals(detail.get("found"))) {
            return Objects.toString(detail.get("message"), "未找到匹配的订单，请确认订单号是否正确。");
        }
        String status = Objects.toString(detail.get("status"), "");
        String statusText = Objects.toString(detail.get("statusText"), "未知");
        boolean refundable = "paid".equalsIgnoreCase(status);
        StringBuilder reply = new StringBuilder();
        reply.append("这笔订单当前").append(refundable ? "可以进入退款/售后处理" : "不满足退款处理条件").append("。");
        reply.append("\n- 订单号：").append(Objects.toString(detail.get("orderNo"), "无"));
        reply.append("\n- 景点：").append(Objects.toString(detail.get("spotName"), "未命名景点"));
        reply.append("\n- 当前状态：").append(statusText);
        reply.append("\n- 判断依据：平台规则中只有已支付订单允许退款处理。");
        if (refundable) {
            reply.append("\n下一步：请到订单详情页查看取消或售后入口；退款金额、到账时间和最终结果以页面规则及后台处理为准。");
        } else {
            reply.append("\n下一步：建议先在订单详情页确认当前可执行按钮，如状态已变化请以页面展示为准。");
        }
        return reply.toString();
    }

    private String formatOrderDetailReply(Map<String, Object> detail) {
        if (Boolean.FALSE.equals(detail.get("found"))) {
            return Objects.toString(detail.get("message"), "未找到匹配的订单，请确认订单号是否正确。");
        }
        StringBuilder reply = new StringBuilder("已查询到这笔订单：");
        reply.append("\n- 景点：").append(Objects.toString(detail.get("spotName"), "未命名景点"));
        reply.append("\n- 状态：").append(Objects.toString(detail.get("statusText"), "未知"));
        reply.append("\n- 订单号：").append(Objects.toString(detail.get("orderNo"), "无"));
        reply.append("\n- 出行日期：").append(Objects.toString(detail.get("visitDate"), "未设置"));
        reply.append("\n- 数量：").append(Objects.toString(detail.get("quantity"), "未确认"));
        reply.append("\n- 总金额：").append(Objects.toString(detail.get("totalPrice"), "未确认"));
        if (Boolean.TRUE.equals(detail.get("canPay"))) {
            reply.append("\n下一步：这笔订单当前可以继续支付，具体以订单页按钮为准。");
        } else if (Boolean.TRUE.equals(detail.get("canCancel"))) {
            reply.append("\n下一步：如行程有变，可以到订单页查看取消或售后入口。");
        } else {
            reply.append("\n下一步：建议到订单详情页查看当前可执行操作。");
        }
        return reply.toString();
    }

    private long parseLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(Objects.toString(value, "0"));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
