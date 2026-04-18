package com.travel.service.ai.guardrail;

import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.config.ai.AiProperties;
import com.travel.config.cache.RedisKeyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * AI 风控服务，统一处理限流、登录要求和输入边界。
 */
@Service
@RequiredArgsConstructor
public class AiGuardrailService {

    /**
     * Redis 访问入口，用于限流计数。
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * AI 配置，用于读取输入限制与限流阈值。
     */
    private final AiProperties aiProperties;

    /**
     * 校验当前请求的频率限制。
     *
     * @param sessionId 会话 ID
     * @param clientIp 客户端 IP
     */
    public void enforceRateLimit(String sessionId, String clientIp) {
        String minuteBucket = String.valueOf(System.currentTimeMillis() / 60000);
        if (StringUtils.hasText(clientIp)) {
            long ipCount = incrementWithOneMinuteTtl(RedisKeyManager.aiGuardrailRateLimitIp(clientIp, minuteBucket));
            if (ipCount > aiProperties.getGuardrail().getRateLimitIpPerMinute()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "请求过于频繁，请稍后重试");
            }
        }
        long sessionCount = incrementWithOneMinuteTtl(RedisKeyManager.aiGuardrailRateLimitSession(sessionId, minuteBucket));
        if (sessionCount > aiProperties.getGuardrail().getRateLimitSessionPerMinute()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "会话请求过于频繁，请稍后重试");
        }
    }

    /**
     * 校验并截断用户消息。
     *
     * @param message 原始消息
     * @return 规范化后的消息
     */
    public String sanitizeUserMessage(String message) {
        if (!StringUtils.hasText(message)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "消息内容不能为空");
        }
        String trimmed = message.trim();
        int maxLength = Math.max(1, aiProperties.getChat().getMaxInputLength());
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    /**
     * 拦截未登录时的个人数据查询请求。
     *
     * @param message 用户消息
     * @param userId 用户 ID
     */
    public void ensureLoginIfNeeded(String message, Long userId) {
        if (userId != null || !StringUtils.hasText(message)) {
            return;
        }
        String content = message.trim();
        boolean personalQuery = containsAny(content,
                "我的订单", "帮我查订单", "查一下订单", "历史订单", "我的收藏",
                "我的账号", "我的资料", "我的信息", "消费记录", "购买记录",
                "我的单子", "单子都列出来", "购票记录", "我的偏好", "我的画像",
                "我喜欢什么", "我适合什么", "浏览行为", "收藏行为")
                || content.matches(".*[Tt]\\d{10,}.*");
        if (personalQuery) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "请先登录后再查询个人订单、收藏和账号信息");
        }
    }

    /**
     * 拦截显式提示词窃取和越权请求。
     *
     * @param message 用户消息
     */
    public void ensureNoPromptInjection(String message) {
        String lower = message == null ? "" : message.toLowerCase();
        if (containsAny(lower, "system prompt", "提示词", "系统提示", "开发者消息", "jailbreak", "输出你的指令")) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该请求涉及系统内部策略，不予处理");
        }
    }

    /**
     * 对指定限流键执行自增，并在首次出现时写入 1 分钟过期时间。
     *
     * @param key 限流键
     * @return 当前分钟桶内计数
     */
    private long incrementWithOneMinuteTtl(String key) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, 1, TimeUnit.MINUTES);
        }
        return count == null ? 0L : count;
    }

    /**
     * 判断文本是否包含任一命中关键词。
     *
     * @param source 原始文本
     * @param keywords 关键词列表
     * @return 是否命中
     */
    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
