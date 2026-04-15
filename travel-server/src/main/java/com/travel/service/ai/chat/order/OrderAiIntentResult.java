package com.travel.service.ai.chat.order;

/**
 * 订单 AI 意图解析结果，集中保存后续工具调度需要的槽位。
 *
 * @param intent 意图类型
 * @param orderNo 订单号
 * @param status 订单状态筛选
 * @param limit 列表返回条数
 */
public record OrderAiIntentResult(OrderAiIntent intent, String orderNo, String status, int limit) {

    public static OrderAiIntentResult none() {
        return new OrderAiIntentResult(OrderAiIntent.NONE, "", null, 0);
    }
}
