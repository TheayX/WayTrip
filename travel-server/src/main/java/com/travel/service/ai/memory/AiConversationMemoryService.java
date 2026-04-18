package com.travel.service.ai.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.config.ai.AiProperties;
import com.travel.config.cache.RedisKeyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AI 会话记忆服务，集中管理最近轮次消息。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiConversationMemoryService {

    /**
     * Redis 访问入口，用于保存会话摘要和历史。
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * JSON 序列化工具，用于读写历史消息。
     */
    private final ObjectMapper objectMapper;

    /**
     * AI 配置，用于读取保留轮数和过期时间。
     */
    private final AiProperties aiProperties;

    /**
     * 读取会话历史。
     *
     * @param sessionId 会话 ID
     * @return 最近轮次消息
     */
    public List<AiConversationTurn> loadHistory(String sessionId) {
        Object cached = redisTemplate.opsForValue().get(RedisKeyManager.aiConversationSession(sessionId));
        if (!(cached instanceof String json) || !StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<AiConversationTurn>>() {
            });
        } catch (Exception e) {
            log.warn("读取 AI 会话历史失败, sessionId={}", sessionId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 保存当前轮对话。
     *
     * @param sessionId 会话 ID
     * @param history 原始历史
     * @param userMessage 用户消息
     * @param assistantReply 助手回复
     */
    public void saveConversation(String sessionId, List<AiConversationTurn> history, String userMessage, String assistantReply) {
        List<AiConversationTurn> updated = new ArrayList<>(history);
        updated.add(new AiConversationTurn("user", userMessage));
        updated.add(new AiConversationTurn("assistant", assistantReply));
        // 这里按“轮次 x 2 条消息”裁剪，保证保留的是最近完整问答对。
        int maxMessages = Math.max(1, aiProperties.getMemory().getHistoryRounds()) * 2;
        if (updated.size() > maxMessages) {
            updated = new ArrayList<>(updated.subList(updated.size() - maxMessages, updated.size()));
        }
        try {
            redisTemplate.opsForValue().set(
                    RedisKeyManager.aiConversationSession(sessionId),
                    objectMapper.writeValueAsString(updated),
                    aiProperties.getMemory().getTtlMinutes(),
                    TimeUnit.MINUTES
            );
            redisTemplate.opsForValue().set(
                    RedisKeyManager.aiConversationSummary(sessionId),
                    updated.size(),
                    aiProperties.getMemory().getTtlMinutes(),
                    TimeUnit.MINUTES
            );
        } catch (Exception e) {
            log.warn("保存 AI 会话历史失败, sessionId={}", sessionId, e);
        }
    }

    /**
     * 清理指定会话。
     *
     * @param sessionId 会话 ID
     */
    public void clearSession(String sessionId) {
        redisTemplate.delete(RedisKeyManager.aiConversationSession(sessionId));
        redisTemplate.delete(RedisKeyManager.aiConversationSummary(sessionId));
    }

    /**
     * 获取会话消息数。
     *
     * @param sessionId 会话 ID
     * @return 消息数
     */
    public int countMessages(String sessionId) {
        Object cached = redisTemplate.opsForValue().get(RedisKeyManager.aiConversationSummary(sessionId));
        if (cached instanceof Number number) {
            return number.intValue();
        }
        return loadHistory(sessionId).size();
    }

    /**
     * 判断会话是否存在。
     *
     * @param sessionId 会话 ID
     * @return 是否存在
     */
    public boolean exists(String sessionId) {
        Boolean exists = redisTemplate.hasKey(RedisKeyManager.aiConversationSession(sessionId));
        return Boolean.TRUE.equals(exists);
    }
}
