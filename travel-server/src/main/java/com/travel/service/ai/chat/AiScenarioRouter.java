package com.travel.service.ai.chat;

import com.travel.enums.ai.AiScenarioType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * AI 场景路由器，负责将请求归类到稳定场景。
 */
@Service
public class AiScenarioRouter {

    /**
     * 根据用户消息和来源信息推断 AI 场景。
     *
     * @param userMessage 用户消息
     * @param scenarioHint 前端弱提示
     * @param sourcePage 来源页面
     * @return 场景类型
     */
    public AiScenarioType route(String userMessage, String scenarioHint, String sourcePage) {
        String merged = String.join(" ", normalize(userMessage), normalize(scenarioHint), normalize(sourcePage)).toLowerCase();
        if (containsAny(merged, "退款", "退票", "改签", "订单", "售后", "赔付")) {
            return AiScenarioType.ORDER_ADVISOR;
        }
        if (containsAny(merged, "为什么推荐", "推荐理由", "解释推荐")) {
            return AiScenarioType.RECOMMENDATION_EXPLAINER;
        }
        if (containsAny(merged, "画像", "偏好", "我喜欢什么")) {
            return AiScenarioType.USER_PROFILE_ANALYZER;
        }
        if (containsAny(merged, "运营", "趋势", "统计分析", "汇总")) {
            return AiScenarioType.OPERATION_ANALYZER;
        }
        if (containsAny(merged, "推荐", "行程", "路线", "去哪玩", "怎么玩", "预算")) {
            return AiScenarioType.TRAVEL_PLANNER;
        }
        return AiScenarioType.CUSTOMER_SERVICE;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
