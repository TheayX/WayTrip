package com.travel.service.ai.chat;

import com.travel.config.ai.AiProperties;
import com.travel.enums.ai.AiScenarioType;
import com.travel.service.ai.memory.AiConversationTurn;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 提示词服务，集中管理系统提示词和历史拼装。
 */
@Service
@RequiredArgsConstructor
public class AiPromptService {

    private final AiProperties aiProperties;

    /**
     * 构建系统提示词。
     *
     * @param scenario 场景类型
     * @return 系统提示词
     */
    public String buildSystemPrompt(AiScenarioType scenario) {
        StringBuilder prompt = new StringBuilder(aiProperties.getChat().getSystemPrompt());
        switch (scenario) {
            case ORDER_ADVISOR -> prompt.append("\n补充要求：优先说明订单状态、售后入口和下一步操作，不得编造金额与时效。");
            case TRAVEL_PLANNER -> prompt.append("\n补充要求：优先给出旅游建议、时间安排和玩法思路，回答要简洁可执行。");
            case RECOMMENDATION_EXPLAINER -> prompt.append("\n补充要求：解释推荐理由时，只能基于真实推荐结果和用户行为摘要。");
            case USER_PROFILE_ANALYZER -> prompt.append("\n补充要求：用户画像结论必须谨慎，不要夸大推断。");
            case OPERATION_ANALYZER -> prompt.append("\n补充要求：运营分析必须以真实统计数据为依据。");
            default -> prompt.append("\n补充要求：优先解决用户问题，信息不足时明确说明缺失信息。");
        }
        return prompt.toString();
    }

    /**
     * 将最近轮次历史压缩成一段文本，避免第一阶段引入过重的消息模型拼装。
     *
     * @param history 最近会话历史
     * @param currentMessage 当前消息
     * @return 合并后的用户输入
     */
    public String buildUserPrompt(List<AiConversationTurn> history, String currentMessage) {
        StringBuilder prompt = new StringBuilder();
        if (history != null && !history.isEmpty()) {
            prompt.append("最近对话上下文：\n");
            for (AiConversationTurn turn : history) {
                prompt.append(turn.getRole()).append(": ").append(turn.getContent()).append('\n');
            }
            prompt.append('\n');
        }
        prompt.append("当前用户问题：\n").append(currentMessage);
        return prompt.toString();
    }
}
