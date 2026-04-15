package com.travel.service.ai.chat.intent;

import com.travel.enums.ai.AiScenarioType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 全局场景规则降级路由，仅在 AI 分类不可用或低置信度时使用。
 */
@Component
public class AiRuleBasedScenarioFallback {

    /**
     * 根据用户消息、前端弱提示和来源页面降级推断场景。
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
        if (containsAny(merged, "攻略", "避坑", "玩法", "怎么玩")) {
            return AiScenarioType.GUIDE_QA;
        }
        if (containsAny(merged, "景点", "门票", "开放时间", "地址", "评分", "适合亲子", "适合情侣")) {
            return AiScenarioType.SPOT_QA;
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
