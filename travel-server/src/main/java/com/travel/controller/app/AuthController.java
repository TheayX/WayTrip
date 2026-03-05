package com.travel.controller.app;

import com.travel.common.result.ApiResponse;
import com.travel.dto.auth.*;
import com.travel.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 用户端认证接口
 */
@Tag(name = "用户端-认证", description = "用户登录、注册相关接口")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "微信登录")
    @PostMapping("/wx-login")
    public ApiResponse<WxLoginResponse> wxLogin(@Valid @RequestBody WxLoginRequest request) {
        return ApiResponse.success(authService.wxLogin(request.getCode()));
    }

    @Operation(summary = "小程序端绑定手机号（新用户注册或匹配已有账户合并openid）")
    @PostMapping("/wx-bind-phone")
    public ApiResponse<LoginResponse> wxBindPhone(@Valid @RequestBody WxBindPhoneRequest request) {
        return ApiResponse.success(authService.wxBindPhone(request));
    }

    @Operation(summary = "Web端注册")
    @PostMapping("/web-register")
    public ApiResponse<LoginResponse> webRegister(@Valid @RequestBody WebRegisterRequest request) {
        return ApiResponse.success(authService.webRegister(request));
    }

    @Operation(summary = "Web端登录")
    @PostMapping("/web-login")
    public ApiResponse<LoginResponse> webLogin(@Valid @RequestBody WebLoginRequest request) {
        return ApiResponse.success(authService.webLogin(request));
    }
}
