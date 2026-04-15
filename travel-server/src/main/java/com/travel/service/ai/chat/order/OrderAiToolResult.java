package com.travel.service.ai.chat.order;

import java.util.Map;

/**
 * 订单 AI 工具执行结果，供回复生成器统一消费。
 *
 * @param intent 订单意图
 * @param payload 工具返回内容
 */
public record OrderAiToolResult(OrderAiIntent intent, Map<String, Object> payload) {
}
