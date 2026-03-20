package com.travel.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 聊天请求
 */
@Data
public class AiChatRequest {

    @NotBlank(message = "消息内容不能为空")
    private String message;

    @NotBlank(message = "会话ID不能为空")
    private String sessionId;
}
