package com.travel.service.ai.chat;

import com.travel.enums.ai.AiScenarioType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * AI 场景路由器，负责将请求简单归类到核心场景。
 */
@Component
public class AiScenarioRouter {

    /**
     * 根据显式提示、来源页面和消息关键词推断当前对话场景。
     * <p>
     * 判定顺序遵循“前端显式 hint > 页面来源 > 用户文本”，避免模型链路在已知场景下再次猜测。
     *
     * @param userMessage 用户消息
     * @param scenarioHint 前端显式场景提示
     * @param sourcePage 来源页面标识
     * @return 推断后的场景类型
     */
    public AiScenarioType route(String userMessage, String scenarioHint, String sourcePage) {
        if (StringUtils.hasText(scenarioHint)) {
            try {
                return AiScenarioType.valueOf(scenarioHint.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        
        if (StringUtils.hasText(sourcePage)) {
            String page = sourcePage.toLowerCase();
            if (page.contains("order")) return AiScenarioType.ORDER_ADVISOR;
            if (page.contains("spot") || page.contains("detail")) return AiScenarioType.SPOT_QA;
            if (page.contains("guide") || page.contains("article")) return AiScenarioType.GUIDE_QA;
            if (page.contains("planner") || page.contains("plan")) return AiScenarioType.TRAVEL_PLANNER;
            if (page.contains("profile") || page.contains("user")) return AiScenarioType.USER_PROFILE_ANALYZER;
            if (page.contains("admin") || page.contains("operation")) return AiScenarioType.OPERATION_ANALYZER;
        }

        if (StringUtils.hasText(userMessage)) {
            String msg = userMessage.toLowerCase();
            if (msg.contains("订单") || msg.contains("退款") || msg.contains("付款")) {
                return AiScenarioType.ORDER_ADVISOR;
            }
            if (msg.contains("推荐") || msg.contains("去哪")) {
                return AiScenarioType.RECOMMENDATION_EXPLAINER;
            }
            if (msg.contains("路线") || msg.contains("规划") || msg.contains("行程")) {
                return AiScenarioType.TRAVEL_PLANNER;
            }
            if (msg.contains("收藏") || msg.contains("偏好") || msg.contains("画像") || msg.contains("我喜欢")) {
                return AiScenarioType.USER_PROFILE_ANALYZER;
            }
            if (msg.contains("攻略") || msg.contains("怎么玩")) {
                return AiScenarioType.GUIDE_QA;
            }
            if (msg.contains("景点") || msg.contains("门票") || msg.contains("好玩吗")) {
                return AiScenarioType.SPOT_QA;
            }
        }

        return AiScenarioType.CUSTOMER_SERVICE;
    }
}
