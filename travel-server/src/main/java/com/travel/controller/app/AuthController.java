package com.travel.controller.app;

import com.travel.common.result.ApiResponse;
import com.travel.dto.auth.*;
import com.travel.service.AuthService;
import com.travel.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 用户端认证接口
 * 用户资料相关路由仅保留兼容能力，主入口为 /api/v1/user/*
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

    @Operation(summary = "小程序端第一步校验手机号密码")
    @PostMapping("/wx-prepare-bind-phone")
    public ApiResponse<WxPrepareBindPhoneResponse> prepareWxBindPhone(@Valid @RequestBody WxPrepareBindPhoneRequest request) {
        return ApiResponse.success(authService.prepareWxBindPhone(request));
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

    @Deprecated
    @Operation(summary = "获取当前用户信息（兼容路由，主入口请使用 /api/v1/user/info）")
    @GetMapping("/user-info")
    public ApiResponse<UserInfoResponse> getUserInfo() {
        Long userId = UserContext.getUserId();
        return ApiResponse.success(authService.getUserInfo(userId));
    }

    @Deprecated
    @Operation(summary = "更新用户信息（兼容路由，主入口请使用 /api/v1/user/info）")
    @PutMapping("/user-info")
    public ApiResponse<Void> updateUserInfo(@Valid @RequestBody UpdateUserInfoRequest request) {
        Long userId = UserContext.getUserId();
        authService.updateUserInfo(userId, request);
        return ApiResponse.success(null);
    }

    @Deprecated
    @Operation(summary = "修改密码（兼容路由，主入口请使用 /api/v1/user/password）")
    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = UserContext.getUserId();
        authService.changePassword(userId, request);
        return ApiResponse.success(null);
    }

    @Deprecated
    @Operation(summary = "设置偏好标签（兼容路由，主入口请使用 /api/v1/user/preferences）")
    @PostMapping("/preferences")
    public ApiResponse<Void> setPreferences(@Valid @RequestBody PreferencesRequest request) {
        Long userId = UserContext.getUserId();
        authService.setPreferences(userId, request.getCategoryIds());
        return ApiResponse.success(null);
    }
}
