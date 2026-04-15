package com.travel.service.ai.chat.travel;

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
 * 旅行内容 AI 意图分类器，负责景点问答和攻略问答的结构化理解。
 */
@Service
@RequiredArgsConstructor
public class TravelContentIntentClassifier {

    private static final String SYSTEM_PROMPT = """
            你是 WayTrip 旅行内容意图分类器。你的任务是把景点和攻略问题分类成稳定意图，只输出 JSON。
            不要回答用户问题，不要解释，不要输出 Markdown。
            可选 intent：
            - SPOT_SEARCH：搜索景点、推荐某类景点、找适合人群的景点
            - SPOT_FACT：询问某个景点的价格、开放时间、地址、评分、简介
            - GUIDE_SEARCH：搜索攻略、询问怎么玩、避坑建议、玩法摘要
            - NONE：不属于景点或攻略问答
            输出 JSON 字段：
            {"intent":"...","keyword":"","spotName":"","city":"","limit":5}
            keyword 填最适合用于搜索的关键词；spotName 仅在明确提到具体景点时填写。
            """;

    private final AiJsonIntentClassificationSupport intentClassificationSupport;
    private final TravelContentIntentResolver fallbackResolver;

    /**
     * 分类旅行内容意图。
     *
     * @param userMessage 用户问题
     * @param scenario 全局场景
     * @return 全局意图包
     */
    public AiIntentClassificationResult classify(String userMessage, AiScenarioType scenario) {
        AiIntentClassificationResult fallback = fallbackResolver.resolve(userMessage, scenario);
        return intentClassificationSupport.classify(
                "旅行内容 AI",
                SYSTEM_PROMPT,
                userMessage,
                () -> fallback,
                root -> normalizeClassifiedResult(parseModelJson(root, scenario), fallback)
        );
    }

    AiIntentClassificationResult parseModelReply(String reply, AiScenarioType scenario) throws Exception {
        return parseModelJson(intentClassificationSupport.parseJsonReply(reply), scenario);
    }

    private AiIntentClassificationResult parseModelJson(JsonNode root, AiScenarioType scenario) {
        TravelContentIntent intent = parseIntent(root.path("intent").asText(""));
        String keyword = TravelContentKeywordNormalizer.normalizeSearchKeyword(
                firstText(root.path("keyword").asText(""), root.path("spotName").asText(""))
        );
        String spotName = TravelContentKeywordNormalizer.normalizeSearchKeyword(root.path("spotName").asText(""));
        String city = root.path("city").asText("");
        int limit = normalizeLimit(root.path("limit").asInt(5));
        AiScenarioType resolvedScenario = intent == TravelContentIntent.GUIDE_SEARCH ? AiScenarioType.GUIDE_QA : scenario;
        return toIntentPackage(resolvedScenario, intent, keyword, spotName, city, limit);
    }

    private AiIntentClassificationResult normalizeClassifiedResult(AiIntentClassificationResult classified,
                                                                  AiIntentClassificationResult fallback) {
        TravelContentIntent intent = parseIntent(classified.intent());
        if (intent == TravelContentIntent.NONE) {
            return TravelContentIntent.NONE.name().equals(fallback.intent()) ? classified : fallback;
        }
        String keyword = StringUtils.hasText(classified.slotAsString(AiIntentSlots.KEYWORD))
                ? TravelContentKeywordNormalizer.normalizeSearchKeyword(classified.slotAsString(AiIntentSlots.KEYWORD))
                : fallback.slotAsString(AiIntentSlots.KEYWORD);
        String spotName = StringUtils.hasText(classified.slotAsString(AiIntentSlots.SPOT_NAME))
                ? TravelContentKeywordNormalizer.normalizeSearchKeyword(classified.slotAsString(AiIntentSlots.SPOT_NAME))
                : fallback.slotAsString(AiIntentSlots.SPOT_NAME);
        String city = StringUtils.hasText(classified.slotAsString(AiIntentSlots.CITY))
                ? classified.slotAsString(AiIntentSlots.CITY)
                : fallback.slotAsString(AiIntentSlots.CITY);
        int limit = classified.slotAsInt(AiIntentSlots.LIMIT, fallback.slotAsInt(AiIntentSlots.LIMIT, 5));
        if (!StringUtils.hasText(keyword) && !StringUtils.hasText(spotName)) {
            return fallback;
        }
        return toIntentPackage(classified.scenario(), intent, keyword, spotName, city, normalizeLimit(limit));
    }

    private AiIntentClassificationResult toIntentPackage(AiScenarioType scenario,
                                                        TravelContentIntent intent,
                                                        String keyword,
                                                        String spotName,
                                                        String city,
                                                        int limit) {
        Map<String, Object> slots = new LinkedHashMap<>();
        if (StringUtils.hasText(keyword)) {
            slots.put(AiIntentSlots.KEYWORD, keyword.trim());
        }
        if (StringUtils.hasText(spotName)) {
            slots.put(AiIntentSlots.SPOT_NAME, spotName.trim());
        }
        if (StringUtils.hasText(city)) {
            slots.put(AiIntentSlots.CITY, city.trim());
        }
        if (limit > 0) {
            slots.put(AiIntentSlots.LIMIT, normalizeLimit(limit));
        }
        return new AiIntentClassificationResult(
                scenario,
                intent.name(),
                slots,
                intent == TravelContentIntent.NONE ? 0D : 0.78D,
                false,
                intent != TravelContentIntent.NONE
        );
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

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first.trim() : StringUtils.hasText(second) ? second.trim() : "";
    }

    private int normalizeLimit(int value) {
        if (value <= 0) {
            return 5;
        }
        return Math.min(value, 10);
    }
}
