package com.travel.service.ai;

import com.travel.dto.ai.feedback.SubmitAiFeedbackRequest;

/**
 * AI 反馈服务接口。
 */
public interface AiFeedbackService {

    void submitFeedback(SubmitAiFeedbackRequest request, Long userId);
}
