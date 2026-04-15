package com.travel.service.ai.chat.recommendation;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 推荐解释回复生成器，基于推荐工具结果解释推荐原因。
 */
@Component
public class RecommendationExplainResponseComposer {

    /**
     * 生成推荐解释回复。
     *
     * @param toolResult 工具执行结果
     * @return 用户可见回复
     */
    public String compose(RecommendationExplainToolResult toolResult) {
        return switch (toolResult.intent()) {
            case EXPLAIN_RECOMMENDATIONS -> formatRecommendationExplanation(toolResult.payload());
            case HOT_RECOMMENDATIONS -> formatHotRecommendations(toolResult.payload());
            case SIMILAR_SPOTS -> formatSimilarSpots(toolResult.payload());
            default -> "暂时无法解释这个推荐问题，请换个问法再试。";
        };
    }

    private String formatRecommendationExplanation(Map<String, Object> payload) {
        List<?> spots = extractList(payload, "spots");
        if (spots.isEmpty()) {
            return "暂时没有拿到推荐候选。你可以先浏览或收藏一些景点，推荐理由会更准确。";
        }
        String type = Objects.toString(payload.get("type"), "recommendation");
        StringBuilder reply = new StringBuilder("这批推荐主要基于");
        reply.append("hot".equalsIgnoreCase(type) ? "平台热门趋势" : "你的偏好和历史行为");
        reply.append("生成，推荐理由可以这样理解：");
        for (Object item : spots) {
            if (item instanceof Map<?, ?> row) {
                reply.append("\n- ")
                        .append(Objects.toString(row.get("name"), "未命名景点"))
                        .append("：")
                        .append(Objects.toString(row.get("categoryName"), "景点"))
                        .append("，")
                        .append(Objects.toString(row.get("regionName"), "地区待确认"))
                        .append("，评分 ")
                        .append(Objects.toString(row.get("avgRating"), "暂无"));
            }
        }
        reply.append("\n这些解释只基于系统可见的推荐结果和景点信息，不代表你一定会喜欢。");
        return reply.toString();
    }

    private String formatHotRecommendations(Map<String, Object> payload) {
        List<?> spots = extractList(payload, "spots");
        if (spots.isEmpty()) {
            return "暂时没有拿到热门推荐结果，请稍后再试。";
        }
        StringBuilder reply = new StringBuilder("当前可以优先看看这些热门景点：");
        for (Object item : spots) {
            reply.append(formatSpotLine(item));
        }
        reply.append("\n热门结果会受浏览、收藏、评分等因素影响，实际排序以页面展示为准。");
        return reply.toString();
    }

    private String formatSimilarSpots(Map<String, Object> payload) {
        if (Boolean.TRUE.equals(payload.get("missingSpotId"))) {
            return "要查询相似景点，需要先明确具体景点 ID。你可以在景点详情页或问题里补充景点 ID。";
        }
        List<?> neighbors = extractList(payload, "neighbors");
        if (neighbors.isEmpty()) {
            return "暂时没有找到这个景点的相似推荐。";
        }
        StringBuilder reply = new StringBuilder("和 ");
        reply.append(Objects.toString(payload.get("spotName"), "该景点")).append(" 相似的景点有：");
        for (Object item : neighbors) {
            reply.append(formatSpotLine(item));
        }
        reply.append("\n相似度来自系统相似关系计算，可作为继续浏览参考。");
        return reply.toString();
    }

    private String formatSpotLine(Object item) {
        if (!(item instanceof Map<?, ?> row)) {
            return "";
        }
        Object name = row.containsKey("name") ? row.get("name") : row.get("spotName");
        return "\n- "
                + Objects.toString(name, "未命名景点")
                + "："
                + Objects.toString(row.get("categoryName"), "景点")
                + "，"
                + Objects.toString(row.get("regionName"), "地区待确认")
                + "，评分 "
                + Objects.toString(row.get("avgRating"), "暂无");
    }

    private List<?> extractList(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value instanceof List<?> list ? list : List.of();
    }
}
