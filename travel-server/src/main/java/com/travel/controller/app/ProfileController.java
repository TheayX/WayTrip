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
 * 用户端个人资料接口
 */
@Tag(name = "用户端-个人资料", description = "用户信息管理、偏好设置、密码修改、账户注销等接口")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class ProfileController {

    private final AuthService authService;

    @Operation(summary = "获取用户信息")
    @GetMapping("/info")
    public ApiResponse<UserInfoResponse> getUserInfo() {
        Long userId = UserContext.getUserId();
        return ApiResponse.success(authService.getUserInfo(userId));
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/info")
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

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = UserContext.getUserId();
        authService.changePassword(userId, request);
        return ApiResponse.success();
    }

    @Operation(summary = "注销账户")
    @DeleteMapping("/account")
    public ApiResponse<Void> deactivateAccount() {
        Long userId = UserContext.getUserId();
        authService.deactivateAccount(userId);
        return ApiResponse.success();
    }
}

