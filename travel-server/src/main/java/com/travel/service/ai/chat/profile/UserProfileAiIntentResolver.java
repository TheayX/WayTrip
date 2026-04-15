package com.travel.service.ai.chat.profile;

import com.travel.enums.ai.AiScenarioType;
import com.travel.service.ai.chat.intent.AiIntentClassificationResult;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 用户画像意图规则解析器，基于明确画像问法触发当前用户行为分析。
 */
@Component
public class UserProfileAiIntentResolver {

    /**
     * 解析用户画像意图。
     *
     * @param userMessage 用户问题
     * @return 意图包
     */
    public AiIntentClassificationResult resolve(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return toIntentPackage(UserProfileAiIntent.NONE);
        }
        if (containsAny(userMessage, "偏好", "喜欢什么", "适合什么", "类型", "画像")) {
            return toIntentPackage(UserProfileAiIntent.PREFERENCE_SUMMARY);
        }
        if (containsAny(userMessage, "浏览", "收藏", "评价", "下单", "行为", "足迹")) {
            return toIntentPackage(UserProfileAiIntent.BEHAVIOR_SUMMARY);
        }
        if (containsAny(userMessage, "总结一下我", "分析一下我", "我的旅游", "我更适合")) {
            return toIntentPackage(UserProfileAiIntent.PROFILE_SUMMARY);
        }
        return toIntentPackage(UserProfileAiIntent.NONE);
    }

    private AiIntentClassificationResult toIntentPackage(UserProfileAiIntent intent) {
        return new AiIntentClassificationResult(
                AiScenarioType.USER_PROFILE_ANALYZER,
                intent.name(),
                Map.of(),
                intent == UserProfileAiIntent.NONE ? 0D : 0.7D,
                intent != UserProfileAiIntent.NONE,
                intent != UserProfileAiIntent.NONE
        );
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
