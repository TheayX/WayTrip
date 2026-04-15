package com.travel.service.ai.chat.profile;

import java.util.Map;

/**
 * 用户画像 AI 工具结果。
 *
 * @param intent 意图类型
 * @param payload 工具返回载荷
 */
public record UserProfileAiToolResult(UserProfileAiIntent intent, Map<String, Object> payload) {
}
