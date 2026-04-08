package com.travel.controller.app;

import com.travel.common.result.ApiResponse;
import com.travel.dto.ai.AiChatRequest;
import com.travel.dto.ai.AiChatResponse;
import com.travel.service.AiService;
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
 * 用户端 AI 控制器，负责 AI 客服对话接口。
 */
@Tag(name = "用户端-AI 客服", description = "用户端 AI 客服对话接口")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    // 服务与鉴权依赖

    private final AiService aiService;
    private final JwtUtils jwtUtil;

    /**
     * 处理用户发起的 AI 对话请求，并按需带入当前登录用户上下文。
     */
    @Operation(summary = "AI 客服对话")
    @PostMapping("/chat")
    public ApiResponse<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request, HttpServletRequest httpRequest) {
        Long userId = resolveUserId(httpRequest.getHeader("Authorization"));
        String reply = aiService.chat(
                request.getSessionId().trim(),
                request.getMessage().trim(),
                userId,
                resolveClientIp(httpRequest)
        );
        return ApiResponse.success(new AiChatResponse(reply));
    }

    // 请求上下文解析

    private Long resolveUserId(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring(7).trim();
        if (!StringUtils.hasText(token)) {
            return null;
        }
        return jwtUtil.getUserIdFromToken(token);
    }

    private String resolveClientIp(HttpServletRequest request) {
        // 兼容反向代理场景下的真实客户端 IP 透传。
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
