package com.travel.service.ai.chat.recommendation;

import com.fasterxml.jackson.databind.JsonNode;
import com.travel.enums.ai.AiScenarioType;
import com.travel.service.ai.chat.intent.AiIntentClassificationResult;
import com.travel.service.ai.chat.intent.AiIntentSlots;
import com.travel.service.ai.chat.intent.AiJsonIntentClassificationSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 推荐解释 AI 意图分类器，负责推荐理由、相似景点和热门推荐的结构化理解。
 */
@Service
@RequiredArgsConstructor
public class RecommendationExplainIntentClassifier {

    private static final String SYSTEM_PROMPT = """
            你是 WayTrip 推荐解释意图分类器。你的任务是识别推荐解释类问题，只输出 JSON。
            不要回答用户问题，不要解释，不要输出 Markdown。
            可选 intent：
            - EXPLAIN_RECOMMENDATIONS：询问为什么推荐、推荐理由、是否适合我
            - SIMILAR_SPOTS：询问相似景点、类似景点
            - HOT_RECOMMENDATIONS：询问热门推荐、大家都在看、推荐几个热门景点
            - NONE：不属于推荐解释
            输出 JSON 字段：
            {"intent":"...","spotId":null,"limit":5}
            spotId 只有在用户明确给出景点 ID 时填写数字，否则为 null。
            """;

    private final AiJsonIntentClassificationSupport intentClassificationSupport;
    private final RecommendationExplainIntentResolver fallbackResolver;

    /**
     * 分类推荐解释意图。
     *
     * @param userMessage 用户问题
     * @return 全局意图包
     */
    public AiIntentClassificationResult classify(String userMessage) {
        AiIntentClassificationResult fallback = fallbackResolver.resolve(userMessage);
        return intentClassificationSupport.classify(
                "推荐解释 AI",
                SYSTEM_PROMPT,
                userMessage,
                () -> fallback,
                root -> normalizeClassifiedResult(parseModelJson(root), fallback)
        );
    }

    AiIntentClassificationResult parseModelReply(String reply) throws Exception {
        return parseModelJson(intentClassificationSupport.parseJsonReply(reply));
    }

    private AiIntentClassificationResult parseModelJson(JsonNode root) {
        RecommendationExplainIntent intent = parseIntent(root.path("intent").asText(""));
        Long spotId = root.path("spotId").isNumber() ? root.path("spotId").asLong() : null;
        int limit = normalizeLimit(root.path("limit").asInt(5));
        return toIntentPackage(intent, spotId, limit);
    }

    private AiIntentClassificationResult normalizeClassifiedResult(AiIntentClassificationResult classified,
                                                                  AiIntentClassificationResult fallback) {
        RecommendationExplainIntent intent = parseIntent(classified.intent());
        if (intent == RecommendationExplainIntent.NONE) {
            return RecommendationExplainIntent.NONE.name().equals(fallback.intent()) ? classified : fallback;
        }
        Long spotId = classified.slotAsLong(AiIntentSlots.SPOT_ID, fallback.slotAsLong(AiIntentSlots.SPOT_ID, 0L));
        int limit = classified.slotAsInt(AiIntentSlots.LIMIT, fallback.slotAsInt(AiIntentSlots.LIMIT, 5));
        return toIntentPackage(intent, spotId > 0 ? spotId : null, normalizeLimit(limit));
    }

    private AiIntentClassificationResult toIntentPackage(RecommendationExplainIntent intent, Long spotId, int limit) {
        Map<String, Object> slots = new LinkedHashMap<>();
        if (spotId != null && spotId > 0) {
            slots.put(AiIntentSlots.SPOT_ID, spotId);
        }
        slots.put(AiIntentSlots.LIMIT, normalizeLimit(limit));
        return new AiIntentClassificationResult(
                AiScenarioType.RECOMMENDATION_EXPLAINER,
                intent.name(),
                slots,
                intent == RecommendationExplainIntent.NONE ? 0D : 0.78D,
                intent == RecommendationExplainIntent.EXPLAIN_RECOMMENDATIONS,
                intent != RecommendationExplainIntent.NONE
        );
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

    private int normalizeLimit(int value) {
        if (value <= 0) {
            return 5;
        }
        return Math.min(value, 10);
    }
}
