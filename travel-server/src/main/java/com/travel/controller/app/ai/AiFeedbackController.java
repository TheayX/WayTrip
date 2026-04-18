package com.travel.controller.app.ai;

import com.travel.common.result.ApiResponse;
import com.travel.dto.ai.feedback.SubmitAiFeedbackRequest;
import com.travel.service.ai.AiFeedbackService;
import com.travel.util.security.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端 AI 反馈控制器。
 */
@Tag(name = "用户端-AI 反馈", description = "用户端 AI 回复反馈接口")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiFeedbackController {

    /**
     * AI 反馈服务入口。
     */
    private final AiFeedbackService aiFeedbackService;

    /**
     * JWT 解析工具，用于识别反馈提交人。
     */
    private final JwtUtils jwtUtils;

    /**
     * 提交 AI 回复反馈。
     *
     * @param request 反馈请求
     * @param httpRequest HTTP 请求
     * @return 空响应
     */
    @Operation(summary = "提交 AI 回复反馈")
    @PostMapping("/feedback")
    public ApiResponse<Void> submitFeedback(@Valid @RequestBody SubmitAiFeedbackRequest request,
                                            HttpServletRequest httpRequest) {
        aiFeedbackService.submitFeedback(request, resolveUserId(httpRequest.getHeader("Authorization")));
        return ApiResponse.success(null);
    }

    /**
     * 从请求头解析当前登录用户 ID。
     *
     * @param authorization Authorization 请求头
     * @return 用户 ID；未登录时返回 {@code null}
     */
    private Long resolveUserId(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring(7).trim();
        if (!StringUtils.hasText(token)) {
            return null;
        }
        return jwtUtils.getUserIdFromToken(token);
    }
}
