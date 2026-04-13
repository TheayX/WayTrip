package com.travel.controller.app;

import com.travel.common.result.ApiResponse;
import com.travel.dto.auth.request.ChangePasswordRequest;
import com.travel.dto.auth.request.PreferencesRequest;
import com.travel.dto.auth.request.UpdateUserInfoRequest;
import com.travel.dto.user.response.UserInfoResponse;
import com.travel.service.UserAccountService;
import com.travel.util.web.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 用户端账户控制器，负责资料、偏好与账号设置接口。
 * <p>
 * 所有需要登录后才能操作的个人资料能力统一放在这里，避免认证相关接口与资料接口职责混杂。
 */
@Tag(name = "用户端-个人资料", description = "用户信息管理、偏好设置、密码修改、账户注销等接口")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserAccountController {

    private final UserAccountService userAccountService;

    @Operation(summary = "获取用户信息")
    @GetMapping("/info")
    public ApiResponse<UserInfoResponse> getUserInfo() {
        Long userId = UserContextHolder.getUserId();
        return ApiResponse.success(userAccountService.getUserInfo(userId));
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/info")
    public ApiResponse<Void> updateUserInfo(@Valid @RequestBody UpdateUserInfoRequest request) {
        Long userId = UserContextHolder.getUserId();
        userAccountService.updateUserInfo(userId, request);
        return ApiResponse.success();
    }

    @Operation(summary = "设置偏好标签")
    @PostMapping("/preferences")
    public ApiResponse<Void> setPreferences(@Valid @RequestBody PreferencesRequest request) {
        // 偏好直接影响推荐冷启动结果，因此在账户域统一维护更容易保证一致性。
        Long userId = UserContextHolder.getUserId();
        userAccountService.setPreferences(userId, request.getCategoryIds());
        return ApiResponse.success();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = UserContextHolder.getUserId();
        userAccountService.changePassword(userId, request);
        return ApiResponse.success();
    }

    @Operation(summary = "注销账户")
    @DeleteMapping("/account")
    public ApiResponse<Void> deactivateCurrentAccount() {
        Long userId = UserContextHolder.getUserId();
        userAccountService.deactivateCurrentAccount(userId);
        return ApiResponse.success();
    }
}
