package com.travel.controller.app.ai;

import com.travel.dto.ai.request.AiChatMessageRequest;
import com.travel.service.ai.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 用户端 AI 聊天控制器。
 */
@Tag(name = "用户端-AI 对话", description = "用户端统一 AI 对话入口")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiChatController {

    /**
     * AI 对话服务入口。
     */
    private final AiChatService aiChatService;

    private final AiRequestIdentityResolver aiRequestIdentityResolver;

    /**
     * 统一 AI 对话入口。
     *
     * @param request 聊天请求
     * @param httpRequest HTTP 请求
     * @return SSE 发射器
     */
    @Operation(summary = "AI 对话")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody AiChatMessageRequest request,
                           HttpServletRequest httpRequest) {
        return aiChatService.chat(
                request,
                aiRequestIdentityResolver.resolveUserId(httpRequest),
                aiRequestIdentityResolver.resolveAdminId(httpRequest),
                aiRequestIdentityResolver.resolveClientIp(httpRequest)
        );
    }
}
