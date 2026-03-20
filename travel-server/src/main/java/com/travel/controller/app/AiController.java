package com.travel.controller.app;

import com.travel.common.result.ApiResponse;
import com.travel.dto.ai.AiChatRequest;
import com.travel.dto.ai.AiChatResponse;
import com.travel.service.AiService;
import com.travel.util.JwtUtil;
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
 * AI chat endpoints.
 */
@Tag(name = "User AI", description = "AI chat APIs")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "AI chat")
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
