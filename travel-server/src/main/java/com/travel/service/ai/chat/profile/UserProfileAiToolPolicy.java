package com.travel.service.ai.chat.profile;

import com.travel.service.ai.tool.UserProfileAiTools;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 用户画像 AI 工具策略。
 */
@Component
@RequiredArgsConstructor
public class UserProfileAiToolPolicy {

    private final UserProfileAiTools userProfileAiTools;

    /**
     * 执行用户画像工具策略。
     *
     * @param intent 意图类型
     * @return 工具结果
     */
    public UserProfileAiToolResult execute(UserProfileAiIntent intent) {
        Map<String, Object> payload = switch (intent) {
            case PROFILE_SUMMARY, PREFERENCE_SUMMARY, BEHAVIOR_SUMMARY -> userProfileAiTools.getCurrentUserProfileSummary();
            default -> Map.of();
        };
        return new UserProfileAiToolResult(intent, payload);
    }
}
