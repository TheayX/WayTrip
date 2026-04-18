package com.travel.service.ai.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.config.ai.AiProperties;
import com.travel.config.cache.RedisKeyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于 Redis 的 Spring AI 对话记忆实现。
 * <p>
 * 该实现直接适配 {@link ChatMemory} 接口，供 MessageChatMemoryAdvisor 在模型调用链中自动读写上下文。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisChatMemory implements ChatMemory {

    /**
     * 字符串型 Redis 模板，用于直接读写 JSON。
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * JSON 序列化工具。
     */
    private final ObjectMapper objectMapper;

    /**
     * AI 配置，用于读取记忆时长和保留轮次。
     */
    private final AiProperties aiProperties;

    /**
     * 追加本轮消息到历史，并统一执行裁剪和过期刷新。
     *
     * @param conversationId 会话 ID
     * @param messages 本轮新增消息
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Message> history = get(conversationId);
        history.addAll(messages);
        history = trimHistory(history);

        try {
            String json = objectMapper.writeValueAsString(history);
            String key = RedisKeyManager.aiConversationSession(conversationId);
            long ttlMinutes = aiProperties.getMemory().getTtlMinutes();

            stringRedisTemplate.opsForValue().set(key, json, Duration.ofMinutes(ttlMinutes));
            stringRedisTemplate.opsForValue().set(
                    RedisKeyManager.aiConversationSummary(conversationId),
                    String.valueOf(history.size()),
                    Duration.ofMinutes(ttlMinutes)
            );
        } catch (Exception e) {
            log.error("Failed to save AI memory for session {}", conversationId, e);
        }
    }

    /**
     * 读取指定会话的消息历史。
     *
     * @param conversationId 会话 ID
     * @return 历史消息列表
     */
    @Override
    public List<Message> get(String conversationId) {
        String key = RedisKeyManager.aiConversationSession(conversationId);
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }

        try {
            List<Message> history = objectMapper.readValue(json, new TypeReference<List<Message>>() {});
            return new ArrayList<>(history);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize AI memory for session {}, attempting to return empty list. Error: {}", conversationId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 清空指定会话的记忆数据。
     *
     * @param conversationId 会话 ID
     */
    @Override
    public void clear(String conversationId) {
        stringRedisTemplate.delete(RedisKeyManager.aiConversationSession(conversationId));
        stringRedisTemplate.delete(RedisKeyManager.aiConversationSummary(conversationId));
    }

    /**
     * 裁剪历史消息，避免 ChatMemory 在长会话中无限增长。
     *
     * @param history 原始历史
     * @return 裁剪后的历史
     */
    private List<Message> trimHistory(List<Message> history) {
        int maxRounds = Math.max(1, aiProperties.getMemory().getHistoryRounds());
        int maxMessages = Math.max(8, maxRounds * 6);
        if (history.size() <= maxMessages) {
            return history;
        }
        return new ArrayList<>(history.subList(history.size() - maxMessages, history.size()));
    }
}
