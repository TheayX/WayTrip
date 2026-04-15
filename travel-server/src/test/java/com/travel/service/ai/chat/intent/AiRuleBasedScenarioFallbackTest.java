package com.travel.service.ai.chat.intent;

import com.travel.enums.ai.AiScenarioType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 全局规则兜底路由测试，覆盖画像和运营分析场景。
 */
class AiRuleBasedScenarioFallbackTest {

    private final AiRuleBasedScenarioFallback fallback = new AiRuleBasedScenarioFallback();

    @Test
    void routeProfileQuestionToProfileAnalyzer() {
        AiScenarioType scenario = fallback.route("我的旅游偏好是什么", null, null);

        assertEquals(AiScenarioType.USER_PROFILE_ANALYZER, scenario);
    }

    @Test
    void routeOperationQuestionBeforeOrderAdvisor() {
        AiScenarioType scenario = fallback.route("最近 7 天订单趋势", null, null);

        assertEquals(AiScenarioType.OPERATION_ANALYZER, scenario);
    }
}
