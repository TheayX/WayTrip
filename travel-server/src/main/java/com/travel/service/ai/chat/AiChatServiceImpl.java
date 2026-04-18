package com.travel.service.ai.chat;

import com.travel.dto.ai.request.AiChatMessageRequest;
import com.travel.dto.ai.response.AiChatMessageResponse;
import com.travel.service.ai.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * AI 对话服务实现，对外收口聊天能力。
 */
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    /**
     * AI 对话主编排器。
     */
    private final AiConversationOrchestrator aiConversationOrchestrator;

    /**
     * 将外部请求转发到统一编排链路。
     *
     * @param request 聊天请求
     * @param userId 当前用户 ID
     * @param adminId 当前管理员 ID
     * @param clientIp 客户端 IP
     * @return 聊天响应
     */
    @Override
    public AiChatMessageResponse chat(AiChatMessageRequest request, Long userId, Long adminId, String clientIp) {
        return aiConversationOrchestrator.chat(request, userId, adminId, clientIp);
    }
}
