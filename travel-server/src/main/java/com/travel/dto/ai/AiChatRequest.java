package com.travel.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 客服对话请求参数对象，承接前端会话标识与当前轮用户输入。
 */
@Data
public class AiChatRequest {

    /**
     * 当前轮用户消息内容，由服务层负责做长度截断与风险过滤。
     */
    @NotBlank(message = "消息内容不能为空")
    private String message;

    /**
     * 前端生成并持久化的会话 ID，用于串联 Redis 历史上下文和限流维度。
     */
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;
}
