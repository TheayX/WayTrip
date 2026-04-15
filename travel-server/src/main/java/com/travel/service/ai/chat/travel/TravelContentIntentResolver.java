package com.travel.service.ai.chat.travel;

import com.travel.enums.ai.AiScenarioType;
import com.travel.service.ai.chat.intent.AiIntentClassificationResult;
import com.travel.service.ai.chat.intent.AiIntentSlots;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 旅行内容意图规则兜底，主要负责模型失败时补齐关键词槽位。
 */
@Component
public class TravelContentIntentResolver {

    /**
     * 解析旅行内容兜底意图。
     *
     * @param userMessage 用户问题
     * @param scenario 全局场景
     * @return 全局意图包
     */
    public AiIntentClassificationResult resolve(String userMessage, AiScenarioType scenario) {
        if (!StringUtils.hasText(userMessage)) {
            return toIntentPackage(scenario, TravelContentIntent.NONE, "", 0);
        }
        String keyword = normalizeKeyword(userMessage);
        if (scenario == AiScenarioType.GUIDE_QA || containsAny(userMessage, "攻略", "避坑", "怎么玩", "玩法")) {
            return toIntentPackage(AiScenarioType.GUIDE_QA, TravelContentIntent.GUIDE_SEARCH, keyword, 5);
        }
        if (scenario == AiScenarioType.SPOT_QA || containsAny(userMessage, "景点", "门票", "开放时间", "地址", "评分")) {
            TravelContentIntent intent = containsAny(userMessage, "门票", "开放时间", "地址", "评分", "多少钱")
                    ? TravelContentIntent.SPOT_FACT
                    : TravelContentIntent.SPOT_SEARCH;
            return toIntentPackage(AiScenarioType.SPOT_QA, intent, keyword, 5);
        }
        return toIntentPackage(scenario, TravelContentIntent.NONE, "", 0);
    }

    private AiIntentClassificationResult toIntentPackage(AiScenarioType scenario,
                                                        TravelContentIntent intent,
                                                        String keyword,
                                                        int limit) {
        Map<String, Object> slots = new LinkedHashMap<>();
        if (StringUtils.hasText(keyword)) {
            slots.put(AiIntentSlots.KEYWORD, keyword);
            slots.put(AiIntentSlots.SPOT_NAME, keyword);
        }
        if (limit > 0) {
            slots.put(AiIntentSlots.LIMIT, Math.min(limit, 10));
        }
        return new AiIntentClassificationResult(
                scenario,
                intent.name(),
                slots,
                intent == TravelContentIntent.NONE ? 0D : 0.65D,
                false,
                intent != TravelContentIntent.NONE
        );
    }

    private String normalizeKeyword(String userMessage) {
        String normalized = TravelContentKeywordNormalizer.normalizeSearchKeyword(userMessage)
                .replace("攻略", "")
                .trim();
        return StringUtils.hasText(normalized) ? normalized : userMessage.trim();
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
