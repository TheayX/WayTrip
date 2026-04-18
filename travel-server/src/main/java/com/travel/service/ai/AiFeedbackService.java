package com.travel.service.ai;

import com.travel.dto.ai.feedback.SubmitAiFeedbackRequest;

/**
 * AI 反馈服务接口。
 */
public interface AiFeedbackService {

    /**
     * 提交一条 AI 回复反馈。
     *
     * @param request 反馈请求
     * @param userId 当前用户 ID，可为空
     */
    void submitFeedback(SubmitAiFeedbackRequest request, Long userId);
}
