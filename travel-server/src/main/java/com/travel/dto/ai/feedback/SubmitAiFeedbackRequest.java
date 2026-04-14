package com.travel.dto.ai.feedback;

import com.travel.enums.ai.AiFeedbackType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI 回复反馈请求对象。
 */
@Data
public class SubmitAiFeedbackRequest {

    @NotBlank(message = "消息ID不能为空")
    private String messageId;

    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    @NotNull(message = "反馈类型不能为空")
    private AiFeedbackType feedbackType;

    private String comment;
}
