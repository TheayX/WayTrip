package com.travel.service.ai.chat.operation;

import java.util.Map;

/**
 * 运营分析 AI 工具结果。
 *
 * @param intent 意图类型
 * @param payload 工具返回载荷
 */
public record OperationAiToolResult(OperationAiIntent intent, Map<String, Object> payload) {
}
