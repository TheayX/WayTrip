package com.travel.service.ai;

import com.travel.dto.ai.request.AiChatMessageRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 对话服务接口。
 */
public interface AiChatService {

    /**
     * 处理 AI 对话请求。
     *
     * @param request 聊天请求
     * @param userId 当前登录用户 ID，可为空
     * @param adminId 当前登录管理员 ID，可为空
     * @param clientIp 客户端 IP，可为空
     * @return SSE 响应发射器
     */
    SseEmitter chat(AiChatMessageRequest request, Long userId, Long adminId, String clientIp);
}
