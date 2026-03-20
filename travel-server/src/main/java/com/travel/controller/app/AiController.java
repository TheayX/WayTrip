package com.travel.controller.app;

import com.travel.common.result.ApiResponse;
import com.travel.dto.ai.AiChatRequest;
import com.travel.dto.ai.AiChatResponse;
import com.travel.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 客服接口
 */
@Tag(name = "用户端-AI", description = "AI 客服对话接口")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @Operation(summary = "AI 聊天")
    @PostMapping("/chat")
    public ApiResponse<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        String reply = aiService.chat(request.getMessage().trim());
        return ApiResponse.success(new AiChatResponse(reply));
    }
}

