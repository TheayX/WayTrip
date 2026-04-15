package com.travel.service.ai.chat.planner;

import com.travel.service.ai.chat.intent.AiIntentClassificationResult;

import java.util.List;
import java.util.Map;

/**
 * 行程规划工具执行结果。
 *
 * @param intent 行程规划意图
 * @param intentResult 全局意图包
 * @param spots 候选景点
 * @param guides 候选攻略
 * @param recommendationType 推荐类型
 */
public record TravelPlanToolResult(
        TravelPlanIntent intent,
        AiIntentClassificationResult intentResult,
        List<Map<String, Object>> spots,
        List<Map<String, Object>> guides,
        String recommendationType
) {
}
