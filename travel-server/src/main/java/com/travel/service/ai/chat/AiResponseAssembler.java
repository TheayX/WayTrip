package com.travel.service.ai.chat;

import com.travel.dto.ai.response.AiChatMessageResponse;
import com.travel.dto.ai.response.AiChatStartEvent;
import com.travel.dto.ai.response.AiCitationItem;
import com.travel.dto.ai.response.AiSuggestionItem;
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
     * 组装流式开始事件，尽早把消息元信息下发给前端。
     *
     * @param sessionId 会话 ID
     * @param messageId 消息 ID
     * @param scenario 场景类型
     * @return 开始事件
     */
    public AiChatStartEvent assembleStartEvent(String sessionId, String messageId, AiScenarioType scenario) {
        AiChatStartEvent event = new AiChatStartEvent();
        event.setSessionId(sessionId);
        event.setMessageId(messageId);
        event.setScenario(scenario);
        event.setCreatedAt(System.currentTimeMillis());
        return event;
    }

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
                                          List<AiToolCallItem> toolCalls, List<AiCitationItem> citations) {
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
        if (citations != null && !citations.isEmpty()) {
            response.setCitations(citations);
        }
        return response;
    }

    /**
     * 为不同场景提供默认追问建议，降低用户二次提问成本。
     *
     * @param scenario 场景类型
     * @return 默认建议列表
     */
    private List<AiSuggestionItem> defaultSuggestions(AiScenarioType scenario) {
        return switch (scenario) {
            case ORDER_ADVISOR -> List.of(
                    suggestion("帮我看看订单状态说明", AiScenarioType.ORDER_ADVISOR),
                    suggestion("退款流程怎么走", AiScenarioType.ORDER_ADVISOR),
                    suggestion("订单页里应该看什么", AiScenarioType.ORDER_ADVISOR)
            );
            case SPOT_QA -> List.of(
                    suggestion("这个景点门票多少钱", AiScenarioType.SPOT_QA),
                    suggestion("开放时间是什么", AiScenarioType.SPOT_QA),
                    suggestion("附近还有哪些景点", AiScenarioType.TRAVEL_PLANNER)
            );
            case GUIDE_QA -> List.of(
                    suggestion("这个地方怎么玩", AiScenarioType.GUIDE_QA),
                    suggestion("有什么避坑建议", AiScenarioType.GUIDE_QA),
                    suggestion("推荐几篇相关攻略", AiScenarioType.GUIDE_QA)
            );
            case TRAVEL_PLANNER -> List.of(
                    suggestion("帮我做 1 日游路线", AiScenarioType.TRAVEL_PLANNER),
                    suggestion("预算 500 的玩法有哪些", AiScenarioType.TRAVEL_PLANNER),
                    suggestion("适合周末出行的景点", AiScenarioType.TRAVEL_PLANNER)
            );
            case RECOMMENDATION_EXPLAINER -> List.of(
                    suggestion("为什么推荐这个景点", AiScenarioType.RECOMMENDATION_EXPLAINER),
                    suggestion("还有相似景点吗", AiScenarioType.RECOMMENDATION_EXPLAINER)
            );
            case USER_PROFILE_ANALYZER -> List.of(
                    suggestion("总结一下我的旅游偏好", AiScenarioType.USER_PROFILE_ANALYZER),
                    suggestion("我最近更适合什么景点", AiScenarioType.TRAVEL_PLANNER),
                    suggestion("我的浏览收藏说明什么", AiScenarioType.USER_PROFILE_ANALYZER)
            );
            case OPERATION_ANALYZER -> List.of(
                    suggestion("看一下运营概览", AiScenarioType.OPERATION_ANALYZER),
                    suggestion("最近 7 天订单趋势", AiScenarioType.OPERATION_ANALYZER),
                    suggestion("当前热门景点有哪些", AiScenarioType.OPERATION_ANALYZER)
            );
            default -> List.of(
                    suggestion("推荐几个适合周末去的景点", AiScenarioType.TRAVEL_PLANNER),
                    suggestion("订单问题可以怎么处理", AiScenarioType.ORDER_ADVISOR)
            );
        };
    }

    /**
     * 构建默认建议项，统一附带稳定的场景提示。
     *
     * @param text 建议文案
     * @param scenario 场景类型
     * @return 建议项
     */
    private AiSuggestionItem suggestion(String text, AiScenarioType scenario) {
        return new AiSuggestionItem(text, scenario.name(), "");
    }
}
