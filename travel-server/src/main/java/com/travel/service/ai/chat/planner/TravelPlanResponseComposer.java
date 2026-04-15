package com.travel.service.ai.chat.planner;

import com.travel.service.ai.chat.intent.AiIntentClassificationResult;
import com.travel.service.ai.chat.intent.AiIntentSlots;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 行程规划回复生成器，基于真实候选景点组织保守行程。
 */
@Component
public class TravelPlanResponseComposer {

    /**
     * 生成行程规划回复。
     *
     * @param toolResult 工具执行结果
     * @return 用户可见回复
     */
    public String compose(TravelPlanToolResult toolResult) {
        if (toolResult.spots().isEmpty()) {
            return "暂时没有拿到可用于规划的景点候选。你可以先补充城市、预算或偏好，我再帮你重新安排。";
        }
        AiIntentClassificationResult intent = toolResult.intentResult();
        int days = Math.max(1, intent.slotAsInt(AiIntentSlots.DAYS, 1));
        int budget = intent.slotAsInt(AiIntentSlots.BUDGET, 0);
        String group = intent.slotAsString(AiIntentSlots.GROUP);
        String preferences = intent.slotAsString(AiIntentSlots.PREFERENCES);

        StringBuilder reply = new StringBuilder("给你一版基于系统真实景点候选的保守行程：");
        reply.append("\n- 天数：").append(days).append(" 天");
        if (budget > 0) {
            reply.append("\n- 预算参考：").append(budget).append(" 元，实际花费以门票、交通和页面价格为准");
        }
        if (StringUtils.hasText(group)) {
            reply.append("\n- 出行人群：").append(group);
        }
        if (StringUtils.hasText(preferences)) {
            reply.append("\n- 偏好：").append(preferences);
        }
        reply.append("\n\n推荐安排：");

        List<Map<String, Object>> spots = toolResult.spots();
        int cursor = 0;
        for (int day = 1; day <= days; day++) {
            reply.append("\n第 ").append(day).append(" 天：");
            int perDay = Math.max(1, Math.min(3, (int) Math.ceil((double) spots.size() / days)));
            for (int i = 0; i < perDay && cursor < spots.size(); i++, cursor++) {
                Map<String, Object> spot = spots.get(cursor);
                reply.append("\n- ")
                        .append(Objects.toString(spot.get("name"), "未命名景点"))
                        .append("：")
                        .append(Objects.toString(spot.get("categoryName"), "景点"))
                        .append("，")
                        .append(Objects.toString(spot.get("regionName"), "地区待确认"))
                        .append("，评分 ")
                        .append(Objects.toString(spot.get("avgRating"), "暂无"));
            }
        }
        if (!toolResult.guides().isEmpty()) {
            reply.append("\n\n可参考攻略：");
            for (Map<String, Object> guide : toolResult.guides()) {
                reply.append("\n- ")
                        .append(Objects.toString(guide.get("title"), "未命名攻略"))
                        .append("，分类：")
                        .append(Objects.toString(guide.get("category"), "未分类"));
            }
        }
        reply.append("\n\n注意：这是一版基于当前系统候选景点的规划草案，门票、开放时间、库存和交通耗时请以详情页及实际出行为准。");
        return reply.toString();
    }
}
