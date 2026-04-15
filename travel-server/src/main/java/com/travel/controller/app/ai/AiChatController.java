package com.travel.controller.app.ai;

import com.travel.common.result.ApiResponse;
import com.travel.dto.ai.request.AiChatMessageRequest;
import com.travel.dto.ai.response.AiChatMessageResponse;
import com.travel.service.ai.AiChatService;
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
 * 用户端 AI 聊天控制器。
 */
@Tag(name = "用户端-AI 对话", description = "用户端统一 AI 对话入口")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;
    private final JwtUtils jwtUtils;

    /**
     * 统一 AI 对话入口。
     *
     * @param request 聊天请求
     * @param httpRequest HTTP 请求
     * @return 聊天响应
     */
    @Operation(summary = "AI 对话")
    @PostMapping("/chat")
    public ApiResponse<AiChatMessageResponse> chat(@Valid @RequestBody AiChatMessageRequest request,
                                                   HttpServletRequest httpRequest) {
        AiChatMessageResponse response = aiChatService.chat(
                request,
                resolveUserId(resolveToken(httpRequest.getHeader("Authorization"))),
                resolveAdminId(resolveToken(httpRequest.getHeader("Authorization"))),
                resolveClientIp(httpRequest)
        );
        return ApiResponse.success(response);
    }

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

    private Long resolveUserId(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        return jwtUtils.getUserIdFromToken(token);
    }

    private Long resolveAdminId(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        return jwtUtils.getAdminIdFromToken(token);
    }

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
