package com.travel.service.ai.chat.profile;

import com.travel.dto.user.response.AdminUserDetailResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 用户画像 AI 回复生成器。
 */
@Component
public class UserProfileAiResponseComposer {

    /**
     * 生成用户画像回复。
     *
     * @param toolResult 工具执行结果
     * @return 用户可见回复
     */
    public String compose(UserProfileAiToolResult toolResult) {
        Map<String, Object> payload = toolResult.payload();
        int orderCount = parseInt(payload.get("orderCount"));
        int favoriteCount = parseInt(payload.get("favoriteCount"));
        int ratingCount = parseInt(payload.get("ratingCount"));
        int viewCount = parseInt(payload.get("viewCount"));
        Object preferenceSummary = payload.get("preferenceSummary");
        Object favoriteSummary = payload.get("favoriteSummary");
        Object viewSummary = payload.get("viewSummary");

        if (orderCount + favoriteCount + ratingCount + viewCount == 0 && preferenceSummary == null) {
            return "目前你的浏览、收藏、订单和评价数据还比较少，暂时不能形成稳定画像。可以先收藏或浏览几个感兴趣的景点，我再帮你总结偏好。";
        }

        StringBuilder reply = new StringBuilder("根据你在 WayTrip 的行为数据，我能做一个轻量画像：");
        reply.append("\n- 行为规模：浏览 ").append(viewCount)
                .append(" 次，收藏 ").append(favoriteCount)
                .append(" 个景点，下单 ").append(orderCount)
                .append(" 次，评价 ").append(ratingCount).append(" 次。");
        appendPreference(reply, preferenceSummary);
        appendFavorite(reply, favoriteSummary);
        appendView(reply, viewSummary);
        appendRecentOrders(reply, payload.get("recentOrders"));
        reply.append("\n结论只能表示“倾向于”，不能代表固定偏好；后续浏览、收藏和下单变化后画像也会变化。");
        return reply.toString();
    }

    private void appendPreference(StringBuilder reply, Object value) {
        if (!(value instanceof AdminUserDetailResponse.PreferenceSummary summary)) {
            return;
        }
        List<String> tags = summary.getTags();
        if (tags == null || tags.isEmpty()) {
            return;
        }
        reply.append("\n- 偏好标签：").append(String.join("、", tags)).append("。");
    }

    private void appendFavorite(StringBuilder reply, Object value) {
        if (!(value instanceof AdminUserDetailResponse.FavoriteSummary summary)) {
            return;
        }
        reply.append("\n- 最近收藏：").append(Objects.toString(summary.getLatestSpotName(), "暂无明确景点")).append("。");
    }

    private void appendView(StringBuilder reply, Object value) {
        if (!(value instanceof AdminUserDetailResponse.ViewSummary summary)) {
            return;
        }
        reply.append("\n- 最近浏览：").append(Objects.toString(summary.getLatestSpotName(), "暂无明确景点"));
        if (summary.getAverageDuration() != null && summary.getAverageDuration() > 0) {
            reply.append("，平均停留约 ").append(summary.getAverageDuration()).append(" 秒");
        }
        reply.append("。");
    }

    private void appendRecentOrders(StringBuilder reply, Object value) {
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            return;
        }
        reply.append("\n- 最近订单：");
        list.stream().limit(3).forEach(item -> {
            if (item instanceof AdminUserDetailResponse.RecentOrder order) {
                reply.append("\n  - ")
                        .append(Objects.toString(order.getSpotName(), "未知景点"))
                        .append("，状态：")
                        .append(Objects.toString(order.getStatus(), "unknown"));
            }
        });
    }

    private int parseInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(Objects.toString(value, "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
