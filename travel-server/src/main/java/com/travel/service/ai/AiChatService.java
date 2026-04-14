package com.travel.service.ai;

import com.travel.dto.ai.request.AiChatMessageRequest;
import com.travel.dto.ai.response.AiChatMessageResponse;

/**
 * AI 对话服务接口。
 */
public interface AiChatService {

    /**
     * 处理 AI 对话请求。
     *
     * @param request 聊天请求
     * @param userId 当前登录用户 ID，可为空
     * @param clientIp 客户端 IP，可为空
     * @return 聊天响应
     */
    AiChatMessageResponse chat(AiChatMessageRequest request, Long userId, String clientIp);
}
