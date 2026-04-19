package com.travel.service.ai.rag;

import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * AI 知识层级辅助类。
 *
 * <p>在不调整表结构的前提下，基于现有文档元数据推断知识属于事实、边界还是回答策略层。</p>
 */
public final class AiKnowledgeLayerSupport {

    public static final String FACT = "fact";
    public static final String BOUNDARY = "boundary";
    public static final String STRATEGY = "strategy";

    private AiKnowledgeLayerSupport() {
    }

    /**
     * 基于标题、来源引用和标签推断知识层级。
     *
     * @param title 标题
     * @param sourceRef 来源引用
     * @param tags 标签
     * @return 知识层级
     */
    public static String inferLayer(String title, String sourceRef, String tags) {
        String normalized = normalize(title) + " " + normalize(sourceRef) + " " + normalize(tags);
        if (containsAny(normalized, "策略", "规范", "回答", "trip-planning", "tool", "prompt", "推荐回答", "工具调用")) {
            return STRATEGY;
        }
        if (containsAny(normalized, "policy:", "account:", "隐私", "登录", "售后", "退款", "支付", "规则", "说明", "边界")) {
            return BOUNDARY;
        }
        return FACT;
    }

    /**
     * 返回知识层级的排序优先级，数值越小越优先。
     *
     * @param layer 知识层级
     * @return 排序优先级
     */
    public static int priority(String layer) {
        if (FACT.equals(layer)) {
            return 0;
        }
        if (BOUNDARY.equals(layer)) {
            return 1;
        }
        if (STRATEGY.equals(layer)) {
            return 2;
        }
        return 3;
    }

    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
