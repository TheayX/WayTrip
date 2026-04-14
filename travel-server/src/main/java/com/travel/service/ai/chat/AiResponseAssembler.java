package com.travel.service.ai.chat;

import com.travel.dto.ai.response.AiChatMessageResponse;
import com.travel.dto.ai.response.AiToolCallItem;
import com.travel.enums.ai.AiScenarioType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * AI 响应组装器，统一补齐消息元信息与建议问题。
 */
@Service
public class AiResponseAssembler {

    /**
     * 组装聊天响应。
     *
     * @param sessionId 会话 ID
     * @param messageId 消息 ID
     * @param scenario 场景类型
     * @param reply 模型回复
     * @return 聊天响应
     */
    public AiChatMessageResponse assemble(String sessionId, String messageId, AiScenarioType scenario, String reply,
                                          List<AiToolCallItem> toolCalls) {
        AiChatMessageResponse response = new AiChatMessageResponse();
        response.setSessionId(sessionId);
        response.setMessageId(messageId);
        response.setScenario(scenario);
        response.setReply(StringUtils.hasText(reply) ? reply.trim() : "我暂时无法确认这个问题，请稍后重试。");
        response.setCreatedAt(System.currentTimeMillis());
        response.setSuggestions(defaultSuggestions(scenario));
        if (toolCalls != null && !toolCalls.isEmpty()) {
            response.setToolCalls(toolCalls);
        }
        return response;
    }

    private List<String> defaultSuggestions(AiScenarioType scenario) {
        return switch (scenario) {
            case ORDER_ADVISOR -> List.of("帮我看看订单状态说明", "退款流程怎么走", "订单页里应该看什么");
            case TRAVEL_PLANNER -> List.of("帮我做 1 日游路线", "预算 500 的玩法有哪些", "适合周末出行的景点");
            case RECOMMENDATION_EXPLAINER -> List.of("为什么推荐这个景点", "还有相似景点吗");
            default -> List.of("推荐几个适合周末去的景点", "订单问题可以怎么处理");
        };
    }
}
