package com.travel.service.ai.chat;

import com.travel.enums.ai.AiScenarioType;
import com.travel.service.ai.chat.intent.AiIntentClassificationResult;
import com.travel.service.ai.chat.intent.AiJsonIntentClassificationSupport;
import com.travel.service.ai.chat.intent.AiRuleBasedScenarioFallback;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * AI 场景路由器，负责将请求归类到稳定场景。
 */
@Component
@RequiredArgsConstructor
public class AiScenarioRouter {

    private static final double MIN_CONFIDENCE = 0.55D;

    private static final String SYSTEM_PROMPT = """
            你是 WayTrip 全局场景路由器。你的任务是判断用户问题属于哪个业务场景，只输出 JSON。
            不要回答用户问题，不要解释，不要输出 Markdown。
            可选 scenario：
            - ORDER_ADVISOR：订单查询、订单状态、退款、售后、支付取消、订单号相关问题
            - TRAVEL_PLANNER：行程规划、路线安排、预算玩法、去哪玩、怎么玩
            - RECOMMENDATION_EXPLAINER：推荐理由、相似景点、为什么推荐
            - USER_PROFILE_ANALYZER：用户偏好、画像、适合什么类型景点
            - OPERATION_ANALYZER：运营趋势、统计分析、后台汇总
            - CUSTOMER_SERVICE：平台功能、登录帮助、其他客服兜底
            输出 JSON 字段：
            {"scenario":"...","intent":"","slots":{},"confidence":0.0,"requiresLogin":false,"requiresTool":false}
            confidence 取 0 到 1。涉及个人订单、收藏、画像时 requiresLogin=true。
            """;

    private final AiJsonIntentClassificationSupport intentClassificationSupport;
    private final AiRuleBasedScenarioFallback ruleBasedScenarioFallback;

    /**
     * 根据用户消息和来源信息推断 AI 场景。
     *
     * @param userMessage 用户消息
     * @param scenarioHint 前端弱提示
     * @param sourcePage 来源页面
     * @return 场景类型
     */
    public AiScenarioType route(String userMessage, String scenarioHint, String sourcePage) {
        return classify(userMessage, scenarioHint, sourcePage).scenario();
    }

    /**
     * 返回完整全局意图包，供后续模块逐步迁移使用。
     *
     * @param userMessage 用户消息
     * @param scenarioHint 前端弱提示
     * @param sourcePage 来源页面
     * @return 全局意图包
     */
    public AiIntentClassificationResult classify(String userMessage, String scenarioHint, String sourcePage) {
        AiScenarioType fallbackScenario = ruleBasedScenarioFallback.route(userMessage, scenarioHint, sourcePage);
        AiIntentClassificationResult fallback = AiIntentClassificationResult.of(fallbackScenario, "");
        String classificationInput = """
                用户问题：%s
                前端弱提示：%s
                来源页面：%s
                """.formatted(normalize(userMessage), normalize(scenarioHint), normalize(sourcePage));
        AiIntentClassificationResult classified = intentClassificationSupport.classify(
                "全局场景 AI",
                SYSTEM_PROMPT,
                classificationInput,
                () -> fallback,
                root -> new AiIntentClassificationResult(
                        parseScenario(root.path("scenario").asText("")),
                        root.path("intent").asText(""),
                        Map.of(),
                        normalizeConfidence(root.path("confidence").asDouble(0D)),
                        root.path("requiresLogin").asBoolean(false),
                        root.path("requiresTool").asBoolean(false)
                )
        );
        if (classified.confidence() < MIN_CONFIDENCE && fallbackScenario != AiScenarioType.CUSTOMER_SERVICE) {
            return fallback;
        }
        return classified;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private AiScenarioType parseScenario(String value) {
        if (!StringUtils.hasText(value)) {
            return AiScenarioType.CUSTOMER_SERVICE;
        }
        try {
            return AiScenarioType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return AiScenarioType.CUSTOMER_SERVICE;
        }
    }

    private double normalizeConfidence(double confidence) {
        if (confidence < 0D) {
            return 0D;
        }
        return Math.min(confidence, 1D);
    }
}
