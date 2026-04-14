package com.travel.controller.app.ai;

import com.travel.common.result.ApiResponse;
import com.travel.dto.ai.session.AiSessionSummaryResponse;
import com.travel.dto.ai.session.CreateAiSessionResponse;
import com.travel.service.ai.AiSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端 AI 会话控制器。
 */
@Tag(name = "用户端-AI 会话", description = "用户端 AI 会话管理接口")
@RestController
@RequestMapping("/api/v1/ai/sessions")
@RequiredArgsConstructor
public class AiSessionController {

    private final AiSessionService aiSessionService;

    /**
     * 创建新的 AI 会话。
     *
     * @return 会话创建结果
     */
    @Operation(summary = "创建 AI 会话")
    @PostMapping
    public ApiResponse<CreateAiSessionResponse> createSession() {
        return ApiResponse.success(aiSessionService.createSession());
    }

    /**
     * 获取 AI 会话摘要。
     *
     * @param sessionId 会话 ID
     * @return 会话摘要
     */
    @Operation(summary = "获取 AI 会话摘要")
    @GetMapping("/{sessionId}")
    public ApiResponse<AiSessionSummaryResponse> getSessionSummary(@PathVariable String sessionId) {
        return ApiResponse.success(aiSessionService.getSessionSummary(sessionId));
    }

    /**
     * 清理指定 AI 会话。
     *
     * @param sessionId 会话 ID
     * @return 空响应
     */
    @Operation(summary = "清空 AI 会话")
    @DeleteMapping("/{sessionId}")
    public ApiResponse<Void> clearSession(@PathVariable String sessionId) {
        aiSessionService.clearSession(sessionId);
        return ApiResponse.success(null);
    }
}
