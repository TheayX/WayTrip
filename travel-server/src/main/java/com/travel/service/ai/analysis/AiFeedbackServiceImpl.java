package com.travel.service.ai.analysis;

import com.travel.dto.ai.feedback.SubmitAiFeedbackRequest;
import com.travel.entity.AiFeedback;
import com.travel.mapper.AiFeedbackMapper;
import com.travel.service.ai.AiFeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI 反馈服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiFeedbackServiceImpl implements AiFeedbackService {

    /**
     * AI 反馈持久层。
     */
    private final AiFeedbackMapper aiFeedbackMapper;

    /**
     * 将用户对 AI 回复的评价落库，供后续分析与优化使用。
     *
     * @param request 反馈请求
     * @param userId 当前用户 ID，可为空
     */
    @Override
    public void submitFeedback(SubmitAiFeedbackRequest request, Long userId) {
        AiFeedback feedback = new AiFeedback();
        feedback.setSessionId(request.getSessionId());
        feedback.setMessageId(request.getMessageId());
        feedback.setUserId(userId);
        feedback.setFeedbackType(request.getFeedbackType().name());
        feedback.setComment(request.getComment() == null ? "" : request.getComment().trim());
        aiFeedbackMapper.insert(feedback);
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
