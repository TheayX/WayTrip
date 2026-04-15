package com.travel.service.ai.chat.recommendation;

import com.travel.service.ai.chat.intent.AiIntentClassificationResult;

import java.util.Map;

/**
 * 推荐解释工具执行结果。
 *
 * @param intent 推荐解释意图
 * @param intentResult 全局意图包
 * @param payload 工具返回内容
 */
public record RecommendationExplainToolResult(
        RecommendationExplainIntent intent,
        AiIntentClassificationResult intentResult,
        Map<String, Object> payload
) {
}
