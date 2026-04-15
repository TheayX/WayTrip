package com.travel.service.ai.chat.travel;

import java.util.Map;

/**
 * 旅行内容 AI 工具执行结果。
 *
 * @param intent 旅行内容意图
 * @param payload 工具返回内容
 */
public record TravelContentToolResult(TravelContentIntent intent, Map<String, Object> payload) {
}
