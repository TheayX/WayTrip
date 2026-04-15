package com.travel.service.ai.chat.profile;

import com.travel.enums.ai.AiScenarioType;
import com.travel.service.ai.chat.intent.AiIntentClassificationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 用户画像 AI 意图解析器测试，确保画像问法只在明确命中时触发。
 */
class UserProfileAiIntentResolverTest {

    private final UserProfileAiIntentResolver resolver = new UserProfileAiIntentResolver();

    @Test
    void resolvePreferenceQuestionAsProfileAnalyzer() {
        AiIntentClassificationResult result = resolver.resolve("总结一下我的旅游偏好");

        assertEquals(AiScenarioType.USER_PROFILE_ANALYZER, result.scenario());
        assertEquals(UserProfileAiIntent.PREFERENCE_SUMMARY.name(), result.intent());
        assertTrue(result.requiresLogin());
        assertTrue(result.requiresTool());
    }

    @Test
    void resolveUnrelatedQuestionAsNone() {
        AiIntentClassificationResult result = resolver.resolve("平台有哪些功能");

        assertEquals(UserProfileAiIntent.NONE.name(), result.intent());
    }
}
