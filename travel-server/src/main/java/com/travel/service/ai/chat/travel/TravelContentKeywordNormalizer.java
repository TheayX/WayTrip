package com.travel.service.ai.chat.travel;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 旅行内容关键词规范化工具，避免把问法噪音直接带入景点搜索。
 */
public final class TravelContentKeywordNormalizer {

    private static final List<String> NOISE_WORDS = List.of(
            "帮我", "看看", "查询", "搜索", "查一下", "请问", "一下",
            "景点", "门票多少钱", "门票价格", "多少钱", "票价",
            "开放时间", "营业时间", "开门时间", "闭馆时间", "几点开门", "几点关门",
            "地址在哪里", "在哪里", "在哪", "地址", "评分", "介绍", "简介",
            "怎么玩", "有什么", "吗", "呢", "？", "?"
    );

    private TravelContentKeywordNormalizer() {
    }

    /**
     * 清洗用户或模型抽取出的景点搜索关键词。
     *
     * @param keyword 原始关键词
     * @return 清洗后的关键词
     */
    public static String normalizeSearchKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return "";
        }
        String normalized = keyword.trim().replaceAll("\\s+", "");
        for (String noiseWord : NOISE_WORDS) {
            normalized = normalized.replace(noiseWord, "");
        }
        return StringUtils.hasText(normalized) ? normalized.trim() : keyword.trim();
    }

    /**
     * 生成保守候选词，搜索失败时只做有限扩展。
     *
     * @param keyword 已清洗关键词
     * @return 候选关键词列表
     */
    public static List<String> buildFallbackKeywords(String keyword) {
        String normalized = normalizeSearchKeyword(keyword);
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }
        Set<String> keywords = new LinkedHashSet<>();
        keywords.add(normalized);
        if (normalized.length() >= 2 && normalized.length() <= 8) {
            keywords.add(normalized + "博物院");
            keywords.add(normalized + "博物馆");
            keywords.add(normalized + "景区");
            keywords.add(normalized + "风景区");
            keywords.add(normalized + "公园");
            keywords.add(normalized + "乐园");
        }
        return new ArrayList<>(keywords);
    }
}
