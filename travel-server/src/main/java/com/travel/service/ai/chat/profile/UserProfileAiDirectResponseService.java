package com.travel.service.ai.chat.profile;

import com.travel.service.ai.chat.intent.AiIntentClassificationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 用户画像 AI 直答服务。
 */
@Service
@RequiredArgsConstructor
public class UserProfileAiDirectResponseService {

    private final UserProfileAiIntentResolver userProfileAiIntentResolver;
    private final UserProfileAiToolPolicy userProfileAiToolPolicy;
    private final UserProfileAiResponseComposer userProfileAiResponseComposer;

    /**
     * 尝试生成用户画像确定性回复。
     *
     * @param userMessage 用户问题
     * @return 可直接返回的回复；无法识别时返回空字符串
     */
    public String tryReply(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return "";
        }
        AiIntentClassificationResult intentResult = userProfileAiIntentResolver.resolve(userMessage);
        UserProfileAiIntent intent = parseIntent(intentResult.intent());
        if (intent == UserProfileAiIntent.NONE) {
            return "";
        }
        return userProfileAiResponseComposer.compose(userProfileAiToolPolicy.execute(intent));
    }

    private UserProfileAiIntent parseIntent(String value) {
        if (!StringUtils.hasText(value)) {
            return UserProfileAiIntent.NONE;
        }
        try {
            return UserProfileAiIntent.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UserProfileAiIntent.NONE;
        }
    }
}
