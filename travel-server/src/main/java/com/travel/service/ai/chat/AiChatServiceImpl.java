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

    private final AiConversationOrchestrator aiConversationOrchestrator;

    @Override
    public AiChatMessageResponse chat(AiChatMessageRequest request, Long userId, String clientIp) {
        return aiConversationOrchestrator.chat(request, userId, clientIp);
    }
}
