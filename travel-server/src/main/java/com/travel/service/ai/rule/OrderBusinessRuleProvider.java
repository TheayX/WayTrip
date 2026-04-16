package com.travel.service.ai.rule;

import com.travel.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单规则提供器，统一基于 Java 真相源输出 AI 可消费的订单状态与售后规则摘要。
 */
@Component
@RequiredArgsConstructor
public class OrderBusinessRuleProvider implements AiBusinessRuleProvider {

    private static final int PAYMENT_TIMEOUT_MINUTES = 5;

    @Override
    public String getDomain() {
        return "order";
    }

    @Override
    public Map<String, Object> describeRules() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paymentTimeoutMinutes", PAYMENT_TIMEOUT_MINUTES);
        result.put("statuses", List.of(
                describeStatus(OrderStatus.PENDING),
                describeStatus(OrderStatus.PAID),
                describeStatus(OrderStatus.CANCELLED),
                describeStatus(OrderStatus.REFUNDED),
                describeStatus(OrderStatus.COMPLETED)
        ));
        result.put("refundPolicy", List.of(
                "仅已支付订单支持退款，其他状态不可退款。",
                "退款金额、到账时间和售后细则以订单页与平台规则为准。"
        ));
        result.put("pageChecklist", List.of(
                "优先查看订单状态、订单号、出行日期和总金额。",
                "再查看是否有支付、取消、退款等可执行按钮。",
                "如需售后，关注页面上的退款说明和规则提示。"
        ));
        result.put("generalOverview", List.of(
                "订单问题通常分为状态说明、支付取消、退款售后和具体订单查询。",
                "通用规则问题不一定需要订单号，具体订单问题建议提供订单号或先查看最近订单。"
        ));
        result.put("disclaimer", "涉及退款金额、到账时间、赔付细则时，必须以订单页和平台规则为准。");
        return result;
    }

    private Map<String, Object> describeStatus(OrderStatus status) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("code", status.getCode());
        row.put("key", status.getKey());
        row.put("description", status.getDescription());
        row.put("canPay", status.canPay());
        row.put("canCancel", status.canCancel());
        row.put("canRefund", status.canRefund());
        row.put("canComplete", status.canComplete());
        row.put("hasRevenue", status.hasRevenue());
        return row;
    }
}
