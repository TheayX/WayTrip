package com.travel.service.ai.chat.travel;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 旅行内容回复生成器，负责景点和攻略事实型回复。
 */
@Component
public class TravelContentResponseComposer {

    /**
     * 生成旅行内容回复。
     *
     * @param toolResult 工具执行结果
     * @return 用户可见回复
     */
    public String compose(TravelContentToolResult toolResult) {
        return switch (toolResult.intent()) {
            case SPOT_FACT -> formatSpotFactReply(toolResult.payload());
            case SPOT_SEARCH -> formatSpotSearchReply(toolResult.payload());
            case GUIDE_SEARCH -> formatGuideSearchReply(toolResult.payload());
            default -> "暂时没有找到匹配的旅行内容，请换个关键词再试。";
        };
    }

    private String formatSpotFactReply(Map<String, Object> payload) {
        List<?> list = extractList(payload);
        if (list.isEmpty()) {
            return "没有查询到匹配的景点。你可以换一个更具体的景点名称再问。";
        }
        Object first = payload.get("detail");
        if (first == null) {
            first = list.get(0);
        }
        if (!(first instanceof Map<?, ?> row)) {
            return "已查询到景点结果，但暂时无法整理详情，请到景点页查看。";
        }
        String factField = Objects.toString(payload.get("factField"), "");
        if (StringUtils.hasText(factField) && !"all".equals(factField)) {
            return formatSingleSpotFactReply(row, factField);
        }
        StringBuilder reply = new StringBuilder("查询到较匹配的景点：");
        reply.append("\n- 名称：").append(Objects.toString(row.get("name"), "未命名景点"));
        reply.append("\n- 门票/价格：").append(Objects.toString(row.get("price"), "以页面展示为准"));
        reply.append("\n- 开放时间：").append(Objects.toString(row.get("openTime"), "以页面展示为准"));
        reply.append("\n- 地址：").append(Objects.toString(row.get("address"), "以页面展示为准"));
        reply.append("\n- 评分：").append(Objects.toString(row.get("avgRating"), "暂无评分"));
        reply.append("\n- 分类：").append(Objects.toString(row.get("categoryName"), "未分类"));
        reply.append("\n- 地区：").append(Objects.toString(row.get("regionName"), "未知地区"));
        reply.append("\n价格、开放安排和可订状态请以景点详情页展示为准。");
        if (list.size() > 1) {
            reply.append("\n如果这不是你要找的景点，可以补充城市或更完整的景点名称。");
        }
        return reply.toString();
    }

    private String formatSingleSpotFactReply(Map<?, ?> row, String factField) {
        String name = Objects.toString(row.get("name"), "这个景点");
        return switch (factField) {
            case "openTime" -> name + "开放时间：" + Objects.toString(row.get("openTime"), "以页面展示为准")
                    + "。开放安排可能调整，请以景点详情页为准。";
            case "price" -> name + "门票/价格：" + Objects.toString(row.get("price"), "以页面展示为准")
                    + "。价格和可订状态请以景点详情页为准。";
            case "address" -> name + "地址：" + Objects.toString(row.get("address"), "以页面展示为准") + "。";
            case "rating" -> name + "评分：" + Objects.toString(row.get("avgRating"), "暂无评分") + "。";
            case "summary" -> name + "简介：" + Objects.toString(row.get("description"), "暂无简介") + "。";
            default -> name + "信息请以景点详情页展示为准。";
        };
    }

    private String formatSpotSearchReply(Map<String, Object> payload) {
        List<?> list = extractList(payload);
        long total = parseLong(payload.get("total"));
        if (list.isEmpty()) {
            return "没有找到匹配的景点。你可以换成城市、景点名称或玩法关键词再试。";
        }
        StringBuilder reply = new StringBuilder("找到 ").append(total).append(" 个相关景点，先给你看这些：");
        for (Object item : list) {
            if (item instanceof Map<?, ?> row) {
                reply.append("\n- ")
                        .append(Objects.toString(row.get("name"), "未命名景点"))
                        .append("，")
                        .append(Objects.toString(row.get("regionName"), "未知地区"))
                        .append("，")
                        .append(Objects.toString(row.get("categoryName"), "未分类"))
                        .append("，评分：")
                        .append(Objects.toString(row.get("avgRating"), "暂无"));
            }
        }
        reply.append("\n价格和开放安排请以景点详情页为准。");
        return reply.toString();
    }

    private String formatGuideSearchReply(Map<String, Object> payload) {
        List<?> list = extractList(payload);
        long total = parseLong(payload.get("total"));
        if (list.isEmpty()) {
            return "没有找到相关攻略。你可以换一个目的地、景点名或玩法关键词再试。";
        }
        StringBuilder reply = new StringBuilder("找到 ").append(total).append(" 篇相关攻略，推荐先看：");
        for (Object item : list) {
            if (item instanceof Map<?, ?> row) {
                reply.append("\n- ")
                        .append(Objects.toString(row.get("title"), "未命名攻略"))
                        .append("，分类：")
                        .append(Objects.toString(row.get("category"), "未分类"))
                        .append("，浏览量：")
                        .append(Objects.toString(row.get("viewCount"), "0"));
            }
        }
        reply.append("\n可以进入攻略详情页查看完整路线、玩法和注意事项。");
        return reply.toString();
    }

    private List<?> extractList(Map<String, Object> payload) {
        Object listValue = payload.get("list");
        return listValue instanceof List<?> list ? list : List.of();
    }

    private long parseLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(Objects.toString(value, "0"));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
