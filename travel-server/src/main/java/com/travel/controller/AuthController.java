package com.travel.controller;

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
 */
@Tag(name = "用户认证")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "微信登录")
    @PostMapping("/wx-login")
    public ApiResponse<LoginResponse> wxLogin(@Valid @RequestBody WxLoginRequest request) {
        return ApiResponse.success(authService.wxLogin(request.getCode()));
    }

    @Operation(summary = "获取用户信息")
    @GetMapping("/user-info")
    public ApiResponse<UserInfoResponse> getUserInfo() {
        Long userId = UserContext.getUserId();
        return ApiResponse.success(authService.getUserInfo(userId));
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/user-info")
    public ApiResponse<Void> updateUserInfo(@RequestBody UpdateUserInfoRequest request) {
        Long userId = UserContext.getUserId();
        authService.updateUserInfo(userId, request);
        return ApiResponse.success();
    }

    @Operation(summary = "设置偏好标签")
    @PostMapping("/preferences")
    public ApiResponse<Void> setPreferences(@Valid @RequestBody PreferencesRequest request) {
        Long userId = UserContext.getUserId();
        authService.setPreferences(userId, request.getTags());
        return ApiResponse.success();
    }
}
