package com.travel.service.ai.chat.recommendation;

import com.travel.service.ai.chat.intent.AiIntentClassificationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 推荐解释直答服务，串联意图分类、工具策略和回复生成。
 */
@Service
@RequiredArgsConstructor
public class RecommendationExplainDirectResponseService {

    private final RecommendationExplainIntentClassifier recommendationExplainIntentClassifier;
    private final RecommendationExplainToolPolicy recommendationExplainToolPolicy;
    private final RecommendationExplainResponseComposer recommendationExplainResponseComposer;

    /**
     * 尝试生成推荐解释确定性回复。
     *
     * @param userMessage 用户问题
     * @return 可直接返回的回复；无法识别时返回空字符串
     */
    public String tryReply(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return "";
        }
        AiIntentClassificationResult intentResult = recommendationExplainIntentClassifier.classify(userMessage);
        RecommendationExplainIntent intent = parseIntent(intentResult.intent());
        if (intent == RecommendationExplainIntent.NONE) {
            return "";
        }
        RecommendationExplainToolResult toolResult = recommendationExplainToolPolicy.execute(intent, intentResult);
        return recommendationExplainResponseComposer.compose(toolResult);
    }

    private RecommendationExplainIntent parseIntent(String value) {
        if (!StringUtils.hasText(value)) {
            return RecommendationExplainIntent.NONE;
        }
        try {
            return RecommendationExplainIntent.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return RecommendationExplainIntent.NONE;
        }
    }
}
