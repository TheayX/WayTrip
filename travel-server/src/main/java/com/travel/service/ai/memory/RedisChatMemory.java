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

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisChatMemory implements ChatMemory {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;

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

    @Override
    public void clear(String conversationId) {
        stringRedisTemplate.delete(RedisKeyManager.aiConversationSession(conversationId));
        stringRedisTemplate.delete(RedisKeyManager.aiConversationSummary(conversationId));
    }

    private List<Message> trimHistory(List<Message> history) {
        int maxRounds = Math.max(1, aiProperties.getMemory().getHistoryRounds());
        int maxMessages = Math.max(8, maxRounds * 6);
        if (history.size() <= maxMessages) {
            return history;
        }
        return new ArrayList<>(history.subList(history.size() - maxMessages, history.size()));
    }
}
