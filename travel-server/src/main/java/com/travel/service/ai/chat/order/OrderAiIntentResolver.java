package com.travel.service.ai.chat.order;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 订单 AI 意图解析器，避免在编排器里堆叠零散关键词判断。
 */
@Component
public class OrderAiIntentResolver {

    private static final Pattern ORDER_NO_PATTERN = Pattern.compile("\\bT\\d{10,}\\d*\\b", Pattern.CASE_INSENSITIVE);

    /**
     * 解析订单相关意图与必要槽位。
     *
     * @param userMessage 用户原始问题
     * @return 意图解析结果
     */
    public OrderAiIntentResult resolve(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return OrderAiIntentResult.none();
        }

        String orderNo = extractOrderNo(userMessage);
        if (StringUtils.hasText(orderNo)) {
            return new OrderAiIntentResult(OrderAiIntent.DETAIL_BY_ORDER_NO, orderNo, null, 0);
        }

        String normalized = normalize(userMessage);
        OrderAiIntent guideIntent = resolveGuideIntent(normalized);
        if (guideIntent != OrderAiIntent.NONE) {
            return new OrderAiIntentResult(guideIntent, "", null, 0);
        }

        if (isOrderListIntent(normalized)) {
            return new OrderAiIntentResult(OrderAiIntent.LIST_ORDERS, "", resolveStatus(normalized), 10);
        }
        return OrderAiIntentResult.none();
    }

    private OrderAiIntent resolveGuideIntent(String normalized) {
        if (containsAny(normalized, "退款", "退票", "售后")
                && containsAny(normalized, "流程", "怎么", "如何", "规则", "说明", "处理")) {
            return OrderAiIntent.GUIDE_REFUND;
        }
        if (containsAny(normalized, "订单页", "订单页面")
                && containsAny(normalized, "看什么", "怎么看", "查看要点", "说明", "要点")) {
            return OrderAiIntent.GUIDE_PAGE;
        }
        if ((containsAny(normalized, "订单状态", "状态说明", "待支付", "已支付", "已取消", "已退款", "已完成")
                && containsAny(normalized, "说明", "含义", "意思", "是什么", "有哪些", "代表什么"))
                || containsAny(normalized, "订单状态说明")) {
            return OrderAiIntent.GUIDE_STATUS;
        }
        return OrderAiIntent.NONE;
    }

    private boolean isOrderListIntent(String normalized) {
        if (!containsAny(normalized, "订单", "单子", "购票记录", "购买记录", "消费记录")) {
            return false;
        }
        if (containsAny(normalized, "详情", "明细", "订单号")) {
            return false;
        }
        return containsAny(normalized, "查看", "查询", "查", "看看", "列出", "显示", "有", "几个", "多少", "全部", "所有", "历史", "最近")
                || StringUtils.hasText(resolveStatus(normalized));
    }

    private String resolveStatus(String normalized) {
        if (containsAny(normalized, "待支付", "未支付")) {
            return "pending";
        }
        if (containsAny(normalized, "已支付", "待出行")) {
            return "paid";
        }
        if (containsAny(normalized, "已完成", "完成")) {
            return "completed";
        }
        if (containsAny(normalized, "已取消", "取消")) {
            return "cancelled";
        }
        if (containsAny(normalized, "已退款", "退款")) {
            return "refunded";
        }
        return null;
    }

    private String extractOrderNo(String userMessage) {
        Matcher matcher = ORDER_NO_PATTERN.matcher(userMessage);
        return matcher.find() ? matcher.group() : "";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", "").toLowerCase();
    }

    private boolean containsAny(String source, String... signals) {
        for (String signal : signals) {
            if (source.contains(signal)) {
                return true;
            }
        }
        return false;
    }
}
