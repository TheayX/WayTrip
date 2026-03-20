package com.travel.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private static final String DEFAULT_SYSTEM_PROMPT = "你是 WayTrip 旅游网站的 AI 客服，请使用简体中文回答。"
            + "回答要简洁、友好，优先围绕旅游、景点、攻略、订单、账号等网站相关问题。"
            + "如果用户问题超出范围，明确说明并引导用户咨询人工客服。";

    @Value("${ollama.base-url:http://127.0.0.1:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.model:qwen2.5:1.5b}")
    private String ollamaModel;

    @Value("${ollama.system-prompt:}")
    private String ollamaSystemPrompt;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String chat(String userMessage) {
        String prompt = StringUtils.hasText(ollamaSystemPrompt) ? ollamaSystemPrompt : DEFAULT_SYSTEM_PROMPT;
        String url = ollamaBaseUrl + "/api/chat";

        Map<String, Object> payload = Map.of(
                "model", ollamaModel,
                "stream", false,
                "messages", List.of(
                        Map.of("role", "system", "content", prompt),
                        Map.of("role", "user", "content", userMessage)
                )
        );

        try {
            String response = restTemplate.postForObject(url, payload, String.class);
            if (!StringUtils.hasText(response)) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "AI 服务暂无响应");
            }
            return parseAssistantContent(response);
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
}

