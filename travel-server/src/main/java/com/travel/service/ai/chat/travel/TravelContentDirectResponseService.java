package com.travel.service.ai.chat.travel;

import com.travel.enums.ai.AiScenarioType;
import com.travel.service.ai.chat.intent.AiIntentClassificationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 旅行内容直答服务，串联意图分类、工具策略和回复生成。
 */
@Service
@RequiredArgsConstructor
public class TravelContentDirectResponseService {

    private final TravelContentIntentClassifier travelContentIntentClassifier;
    private final TravelContentToolPolicy travelContentToolPolicy;
    private final TravelContentResponseComposer travelContentResponseComposer;

    /**
     * 尝试生成旅行内容确定性回复。
     *
     * @param userMessage 用户问题
     * @param scenario 全局场景
     * @return 可直接返回的回复；无法识别时返回空字符串
     */
    public String tryReply(String userMessage, AiScenarioType scenario) {
        if (!StringUtils.hasText(userMessage)) {
            return "";
        }
        AiIntentClassificationResult intentResult = travelContentIntentClassifier.classify(userMessage, scenario);
        TravelContentIntent intent = parseIntent(intentResult.intent());
        if (intent == TravelContentIntent.NONE) {
            return "";
        }
        TravelContentToolResult toolResult = travelContentToolPolicy.execute(intent, intentResult);
        return travelContentResponseComposer.compose(toolResult);
    }

    private TravelContentIntent parseIntent(String value) {
        if (!StringUtils.hasText(value)) {
            return TravelContentIntent.NONE;
        }
        try {
            return TravelContentIntent.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TravelContentIntent.NONE;
        }
    }
}
