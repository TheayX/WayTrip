package com.travel.service.support.recommendation;

import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 浏览来源归类器，统一维护推荐侧可识别的来源桶。
 */
@Component
public class RecommendationViewSourceClassifier {

    public String normalize(String source) {
        if (source == null || source.isBlank()) {
            return "detail";
        }
        return switch (source.trim().toLowerCase(Locale.ROOT)) {
            case "search" -> "search";
            case "recommendation", "discover", "random-pick", "budget-travel",
                "traveler-reviews", "trending-views" -> "recommendation";
            case "home" -> "home";
            case "guide" -> "guide";
            case "detail", "list", "nearby", "similar", "order", "footprint", "favorite", "review" -> "detail";
            default -> "detail";
        };
    }
}
