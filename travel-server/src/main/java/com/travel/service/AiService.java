package com.travel.service;

/**
 * AI 客服服务接口。
 */
public interface AiService {

    /**
     * 处理 AI 客服对话请求。
     *
     * @param sessionId 会话 ID
     * @param userMessage 用户消息
     * @param userId 当前登录用户 ID，可为空
     * @param clientIp 客户端 IP，可为空
     * @return AI 回复内容
     */
    String chat(String sessionId, String userMessage, Long userId, String clientIp);
}
