package com.travel.service.ai.analysis;

import com.travel.dto.ai.feedback.SubmitAiFeedbackRequest;
import com.travel.service.ai.AiFeedbackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI 反馈服务实现。
 */
@Slf4j
@Service
public class AiFeedbackServiceImpl implements AiFeedbackService {

    @Override
    public void submitFeedback(SubmitAiFeedbackRequest request, Long userId) {
        // 第一阶段先将反馈收口到日志，后续再接正式表结构与管理端统计。
        log.info(
                "收到 AI 反馈：userId={}, sessionId={}, messageId={}, feedbackType={}, comment={}",
                userId,
                request.getSessionId(),
                request.getMessageId(),
                request.getFeedbackType(),
                request.getComment()
        );
    }
}
