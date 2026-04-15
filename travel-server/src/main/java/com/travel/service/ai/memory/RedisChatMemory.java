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
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
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
        
        // Retain only the last N messages
        int maxRounds = aiProperties.getMemory().getHistoryRounds();
        int maxMessages = Math.max(1, maxRounds) * 2;
        if (history.size() > maxMessages) {
            history = history.subList(history.size() - maxMessages, history.size());
        }

        try {
            // ObjectMapper can serialize Spring AI Message objects if SpringAiModule is registered, 
            // but we can also rely on generic JSON maps to be safe. We'll use objectMapper which should have correct config.
            String json = objectMapper.writeValueAsString(history);
            String key = RedisKeyManager.aiConversationSession(conversationId);
            long ttlMinutes = aiProperties.getMemory().getTtlMinutes();
            
            stringRedisTemplate.opsForValue().set(key, json, Duration.ofMinutes(ttlMinutes));
            
            // Increment/update count
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
}
