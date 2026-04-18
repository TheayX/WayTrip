package com.travel.controller.app.ai;

import com.travel.dto.ai.request.AiChatMessageRequest;
import com.travel.service.ai.AiChatService;
import com.travel.util.security.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
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

    /**
     * JWT 解析工具，用于识别当前请求身份。
     */
    private final JwtUtils jwtUtils;

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
                resolveUserId(resolveToken(httpRequest.getHeader("Authorization"))),
                resolveAdminId(resolveToken(httpRequest.getHeader("Authorization"))),
                resolveClientIp(httpRequest)
        );
    }

    /**
     * 从 Authorization 请求头中提取 Bearer Token。
     *
     * @param authorization 请求头原始值
     * @return Token；不存在或格式非法时返回 {@code null}
     */
    private String resolveToken(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring(7).trim();
        if (!StringUtils.hasText(token)) {
            return null;
        }
        return token;
    }

    /**
     * 解析普通用户身份。
     *
     * @param token JWT Token
     * @return 用户 ID；解析失败时返回 {@code null}
     */
    private Long resolveUserId(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        return jwtUtils.getUserIdFromToken(token);
    }

    /**
     * 解析管理端身份。
     *
     * @param token JWT Token
     * @return 管理员 ID；解析失败时返回 {@code null}
     */
    private Long resolveAdminId(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        return jwtUtils.getAdminIdFromToken(token);
    }

    /**
     * 按常见反向代理请求头顺序解析客户端真实 IP。
     *
     * @param request HTTP 请求
     * @return 客户端 IP
     */
    private String resolveClientIp(HttpServletRequest request) {
        String[] headers = new String[]{"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                return ip.contains(",") ? ip.split(",")[0].trim() : ip.trim();
            }
        }
        return request.getRemoteAddr();
    }
}
