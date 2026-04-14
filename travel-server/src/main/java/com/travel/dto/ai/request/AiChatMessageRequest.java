package com.travel.dto.ai.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 聊天请求对象。
 */
@Data
public class AiChatMessageRequest {

    /**
     * 会话 ID，由服务端创建、前端持有。
     */
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    /**
     * 用户当前轮消息。
     */
    @NotBlank(message = "消息内容不能为空")
    private String message;

    /**
     * 前端传入的弱场景提示，例如 order/detail。
     */
    private String scenarioHint;

    /**
     * 来源页面标识，用于辅助场景路由。
     */
    private String sourcePage;

    /**
     * 客户端时间戳，仅用于排查链路问题。
     */
    private Long clientTime;
}
