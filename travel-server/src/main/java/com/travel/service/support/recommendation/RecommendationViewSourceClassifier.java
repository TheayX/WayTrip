package com.travel.service.support.recommendation;

import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 浏览来源归类器，统一维护推荐侧可识别的来源桶。
 * <p>
 * 推荐侧不会消费前端所有原始来源值，而是先归并到少量稳定桶，避免统计口径不断膨胀。
 */
@Component
public class RecommendationViewSourceClassifier {

    /**
     * 归一化前端上报的浏览来源。
     *
     * @param source 原始来源
     * @return 推荐侧可识别的标准来源
     */
    public String normalize(String source) {
        if (source == null || source.isBlank()) {
            return "detail";
        }
        // 新入口若未单独纳入规则，默认并回 detail，避免来源缺失导致打分分桶异常。
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
