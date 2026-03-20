package com.travel.service;

/**
 * AI service interface.
 */
public interface AiService {

    /**
     * Chat with assistant.
     */
    String chat(String sessionId, String userMessage, Long userId, String clientIp);
}
