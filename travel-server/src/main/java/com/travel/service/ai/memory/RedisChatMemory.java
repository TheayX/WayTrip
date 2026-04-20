package com.travel.service.ai.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.config.ai.AiProperties;
import com.travel.config.cache.RedisKeyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
            String json = objectMapper.writeValueAsString(history.stream().map(this::toStoredMessage).toList());
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
            List<StoredMessage> history = objectMapper.readValue(json, new TypeReference<List<StoredMessage>>() {});
            if (history == null || history.isEmpty()) {
                return new ArrayList<>();
            }
            List<Message> restored = history.stream().map(this::toRuntimeMessage).toList();
            if (restored.isEmpty()) {
                return new ArrayList<>();
            }
            return new ArrayList<>(restored);
        } catch (IllegalArgumentException | JsonProcessingException e) {
            log.warn("Failed to deserialize AI memory for session {}, clearing broken cache. Error: {}", conversationId, e.getMessage());
            clear(conversationId);
            return new ArrayList<>();
        } catch (Exception e) {
            log.warn("Failed to deserialize AI memory for session {}, attempting to return empty list. Error: {}", conversationId, e.getMessage());
            clear(conversationId);
            return new ArrayList<>();
        }
    }

    /**
     * 将运行时消息转换为稳定可控的缓存结构。
     *
     * @param message 运行时消息
     * @return 缓存消息
     */
    private StoredMessage toStoredMessage(Message message) {
        StoredMessage storedMessage = new StoredMessage();
        storedMessage.setType(message.getMessageType() == null ? "" : message.getMessageType().name());
        storedMessage.setText(message.getText());
        storedMessage.setMetadata(message.getMetadata() == null ? Map.of() : new LinkedHashMap<>(message.getMetadata()));

        if (message instanceof AssistantMessage assistantMessage && assistantMessage.getToolCalls() != null) {
            storedMessage.setToolCalls(assistantMessage.getToolCalls().stream()
                    .map(item -> new StoredToolCall(item.id(), item.type(), item.name(), item.arguments()))
                    .toList());
        }
        if (message instanceof ToolResponseMessage toolResponseMessage && toolResponseMessage.getResponses() != null) {
            storedMessage.setToolResponses(toolResponseMessage.getResponses().stream()
                    .map(item -> new StoredToolResponse(item.id(), item.name(), item.responseData()))
                    .toList());
        }
        return storedMessage;
    }

    /**
     * 将缓存消息重建为 Spring AI 运行时消息。
     *
     * @param storedMessage 缓存消息
     * @return 运行时消息
     */
    private Message toRuntimeMessage(StoredMessage storedMessage) {
        if (storedMessage == null || !org.springframework.util.StringUtils.hasText(storedMessage.getType())) {
            throw new IllegalArgumentException("AI memory message type is blank");
        }

        MessageType messageType = MessageType.valueOf(storedMessage.getType().trim().toUpperCase());
        Map<String, Object> metadata = storedMessage.getMetadata() == null
                ? Map.of()
                : new LinkedHashMap<>(storedMessage.getMetadata());

        return switch (messageType) {
            case USER -> UserMessage.builder()
                    .text(storedMessage.getText() == null ? "" : storedMessage.getText())
                    .metadata(metadata)
                    .build();
            case SYSTEM -> SystemMessage.builder()
                    .text(storedMessage.getText() == null ? "" : storedMessage.getText())
                    .metadata(metadata)
                    .build();
            case ASSISTANT -> new AssistantMessage(
                    storedMessage.getText(),
                    metadata,
                    storedMessage.getToolCalls() == null ? List.of() : storedMessage.getToolCalls().stream()
                            .map(item -> new AssistantMessage.ToolCall(item.id(), item.type(), item.name(), item.arguments()))
                            .toList()
            );
            case TOOL -> new ToolResponseMessage(
                    storedMessage.getToolResponses() == null ? List.of() : storedMessage.getToolResponses().stream()
                            .map(item -> new ToolResponseMessage.ToolResponse(item.id(), item.name(), item.responseData()))
                            .toList(),
                    metadata
            );
        };
    }

    /**
     * 缓存中的消息结构。
     */
    @lombok.Data
    private static class StoredMessage {

        /**
         * 消息类型。
         */
        private String type;

        /**
         * 文本内容。
         */
        private String text;

        /**
         * 元数据。
         */
        private Map<String, Object> metadata = new LinkedHashMap<>();

        /**
         * 助手工具调用记录。
         */
        private List<StoredToolCall> toolCalls = List.of();

        /**
         * 工具响应记录。
         */
        private List<StoredToolResponse> toolResponses = List.of();
    }

    /**
     * 缓存中的工具调用结构。
     */
    private record StoredToolCall(
            String id,
            String type,
            String name,
            String arguments
    ) {
    }

    /**
     * 缓存中的工具响应结构。
     */
    private record StoredToolResponse(
            String id,
            String name,
            String responseData
    ) {
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
