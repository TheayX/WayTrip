package com.travel.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private static final String DEFAULT_SYSTEM_PROMPT =
            "你是 WayTrip 旅游网站的 AI 客服，请使用简体中文回答。"
                    + "回答要简洁、友好，优先围绕旅游、景点、攻略、订单和账号相关问题。"
                    + "不确定时请明确说不确定，并建议用户联系人工客服。";
    private static final String HISTORY_KEY_PREFIX = "ai:chat:session:";
    private static final int DEFAULT_MAX_ROUNDS = 6;
    private static final int DEFAULT_HISTORY_TTL_MINUTES = 30;
    private static final int MAX_SINGLE_MESSAGE_LENGTH = 800;

    @Value("${ollama.base-url:http://127.0.0.1:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.model:qwen2.5:1.5b}")
    private String ollamaModel;

    @Value("${ollama.system-prompt:}")
    private String ollamaSystemPrompt;

    @Value("${ollama.chat.max-history-rounds:6}")
    private Integer maxHistoryRounds;

    @Value("${ollama.chat.history-ttl-minutes:30}")
    private Integer historyTtlMinutes;

    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String chat(String sessionId, String userMessage) {
        String safeSessionId = sanitizeSessionId(sessionId);
        String safeUserMessage = truncateMessage(userMessage);
        IntentType intentType = detectIntent(safeUserMessage);
        String prompt = buildSystemPrompt(intentType);
        String url = ollamaBaseUrl + "/api/chat";

        List<Map<String, String>> historyMessages = loadHistoryMessages(safeSessionId);
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", prompt));
        messages.addAll(historyMessages);
        messages.add(Map.of("role", "user", "content", safeUserMessage));

        Map<String, Object> payload = Map.of(
                "model", ollamaModel,
                "stream", false,
                "messages", messages
        );

        try {
            String response = restTemplate.postForObject(url, payload, String.class);
            if (!StringUtils.hasText(response)) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "AI 服务暂无响应");
            }
            String reply = parseAssistantContent(response);
            saveConversation(safeSessionId, historyMessages, safeUserMessage, reply);
            return reply;
        } catch (RestClientException e) {
            log.error("调用 Ollama 失败, url={}, model={}", url, ollamaModel, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "AI 服务连接失败，请稍后重试");
        }
    }

    private String parseAssistantContent(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String content = root.path("message").path("content").asText("");
            if (!StringUtils.hasText(content)) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "AI 返回内容为空");
            }
            return content.trim();
        } catch (JsonProcessingException e) {
            log.error("解析 Ollama 响应失败: {}", response, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "AI 服务响应解析失败");
        }
    }

    private String buildSystemPrompt(IntentType intentType) {
        String basePrompt = StringUtils.hasText(ollamaSystemPrompt) ? ollamaSystemPrompt : DEFAULT_SYSTEM_PROMPT;
        return basePrompt + "\n\n" + intentType.getPrompt();
    }

    private IntentType detectIntent(String userMessage) {
        String content = userMessage.toLowerCase();
        if (containsAny(content, "退款", "退票", "改签", "取消订单", "售后", "赔付")) {
            return IntentType.ORDER_SERVICE;
        }
        if (containsAny(content, "下单", "支付", "订单", "发票")) {
            return IntentType.ORDER_PROCESS;
        }
        if (containsAny(content, "登录", "注册", "密码", "账号", "手机号", "验证码")) {
            return IntentType.ACCOUNT;
        }
        if (containsAny(content, "景点", "门票", "攻略", "路线", "推荐", "哪里玩", "怎么玩")) {
            return IntentType.TRAVEL_GUIDE;
        }
        return IntentType.GENERAL;
    }

    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> loadHistoryMessages(String sessionId) {
        String cacheKey = HISTORY_KEY_PREFIX + sessionId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (!(cached instanceof String cachedJson) || !StringUtils.hasText(cachedJson)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(
                    cachedJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)
            );
        } catch (Exception e) {
            log.warn("解析会话历史失败, sessionId={}", sessionId, e);
            return new ArrayList<>();
        }
    }

    private void saveConversation(String sessionId, List<Map<String, String>> history, String userMessage, String assistantReply) {
        List<Map<String, String>> updatedHistory = new ArrayList<>(history);
        updatedHistory.add(Map.of("role", "user", "content", userMessage));
        updatedHistory.add(Map.of("role", "assistant", "content", truncateMessage(assistantReply)));

        int maxMessages = Math.max(1, getMaxHistoryRounds()) * 2;
        if (updatedHistory.size() > maxMessages) {
            updatedHistory = new ArrayList<>(updatedHistory.subList(updatedHistory.size() - maxMessages, updatedHistory.size()));
        }

        try {
            String cacheKey = HISTORY_KEY_PREFIX + sessionId;
            String serialized = objectMapper.writeValueAsString(updatedHistory);
            redisTemplate.opsForValue().set(cacheKey, serialized, getHistoryTtlMinutes(), TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("保存会话历史失败, sessionId={}", sessionId, e);
        }
    }

    private Integer getMaxHistoryRounds() {
        return maxHistoryRounds == null || maxHistoryRounds <= 0 ? DEFAULT_MAX_ROUNDS : maxHistoryRounds;
    }

    private Integer getHistoryTtlMinutes() {
        return historyTtlMinutes == null || historyTtlMinutes <= 0 ? DEFAULT_HISTORY_TTL_MINUTES : historyTtlMinutes;
    }

    private String sanitizeSessionId(String sessionId) {
        String safe = sessionId == null ? "" : sessionId.trim();
        if (!StringUtils.hasText(safe)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "会话ID不能为空");
        }
        if (safe.length() > 64) {
            safe = safe.substring(0, 64);
        }
        safe = safe.replaceAll("[^A-Za-z0-9_-]", "");
        if (!StringUtils.hasText(safe)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "会话ID不合法");
        }
        return safe;
    }

    private String truncateMessage(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String trimmed = content.trim();
        if (trimmed.length() <= MAX_SINGLE_MESSAGE_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_SINGLE_MESSAGE_LENGTH);
    }

    private enum IntentType {
        TRAVEL_GUIDE(
                "意图=旅游咨询。优先提供景点玩法、适合人群、行程建议。"
                        + "如涉及门票价格、开放时间等实时信息，提醒用户以页面展示为准。"
        ),
        ORDER_PROCESS(
                "意图=订单流程。优先回答下单、支付、订单状态查询、发票相关步骤。"
                        + "如果用户提供订单号，提醒在订单详情页核对。"
        ),
        ORDER_SERVICE(
                "意图=售后问题。先说明可在“我的订单”中查看订单状态与售后入口。"
                        + "退款规则若无明确数据，不要编造具体金额和到账时间，建议联系人工客服确认。"
        ),
        ACCOUNT(
                "意图=账号问题。回答登录注册、密码找回、手机号绑定等流程，避免要求用户提供敏感信息。"
        ),
        GENERAL(
                "意图=通用咨询。若能关联旅游场景就结合回答，超出网站服务范围时礼貌拒答并建议人工客服。"
        );

        private final String prompt;

        IntentType(String prompt) {
            this.prompt = prompt;
        }

        public String getPrompt() {
            return prompt;
        }
    }
}

