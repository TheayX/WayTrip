package com.travel.service.ai.intent;

import com.travel.enums.ai.AiScenarioType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 全局规则兜底场景路由。
 */
@Component
public class AiScenarioFallback {

    /**
     * 依据来源页和用户文本做兜底场景判断。
     *
     * @param userMessage 用户消息
     * @param scenarioHint 前端提示
     * @param sourcePage 来源页
     * @return 场景类型
     */
    public AiScenarioType route(String userMessage, String scenarioHint, String sourcePage) {
        if (StringUtils.hasText(sourcePage)) {
            String page = sourcePage.toLowerCase();
            if (page.contains("admin") || page.contains("operation")) return AiScenarioType.OPERATION_ANALYZER;
            if (page.contains("order")) return AiScenarioType.ORDER_ADVISOR;
            if (page.contains("spot") || page.contains("detail")) return AiScenarioType.SPOT_QA;
            if (page.contains("guide") || page.contains("article")) return AiScenarioType.GUIDE_QA;
            if (page.contains("recommend") || page.contains("nearby")) return AiScenarioType.RECOMMENDATION_EXPLAINER;
            if (page.contains("planner") || page.contains("plan")) return AiScenarioType.TRAVEL_PLANNER;
            if (page.contains("profile") || page.contains("user")) return AiScenarioType.USER_PROFILE_ANALYZER;
        }
        if (!StringUtils.hasText(userMessage)) {
            return AiScenarioType.CUSTOMER_SERVICE;
        }
        String msg = userMessage.toLowerCase();
        if (msg.contains("趋势") || msg.contains("运营") || msg.contains("热门景点有哪些")) {
            return AiScenarioType.OPERATION_ANALYZER;
        }
        if (msg.contains("画像") || msg.contains("偏好") || msg.contains("我喜欢")) {
            return AiScenarioType.USER_PROFILE_ANALYZER;
        }
        if (msg.contains("订单") || msg.contains("退款") || msg.contains("付款")) {
            return AiScenarioType.ORDER_ADVISOR;
        }
        if (msg.contains("适合") || msg.contains("个性化") || msg.contains("推荐我") || msg.contains("帮我推荐")) {
            return AiScenarioType.TRAVEL_PLANNER;
        }
        if (msg.contains("推荐") || msg.contains("去哪")) {
            return AiScenarioType.RECOMMENDATION_EXPLAINER;
        }
        if (msg.contains("路线") || msg.contains("规划") || msg.contains("行程")) {
            return AiScenarioType.TRAVEL_PLANNER;
        }
        if (msg.contains("攻略") || msg.contains("怎么玩")) {
            return AiScenarioType.GUIDE_QA;
        }
        if (msg.contains("景点") || msg.contains("门票") || msg.contains("好玩吗")) {
            return AiScenarioType.SPOT_QA;
        }
        return AiScenarioType.CUSTOMER_SERVICE;
    }
}
