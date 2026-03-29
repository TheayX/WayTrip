package com.travel.controller.app;

import com.travel.common.result.ApiResponse;
import com.travel.dto.auth.*;
import com.travel.service.UserAccountService;
import com.travel.service.UserAuthService;
import com.travel.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户端认证接口
 * 用户资料相关路由仅保留兼容能力，主入口为 /api/v1/user/*
 */
@Tag(name = "用户端-认证", description = "用户登录、注册相关接口")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserAuthService userAuthService;
    private final UserAccountService userAccountService;

    @Operation(summary = "微信登录")
    @PostMapping("/wx-login")
    public ApiResponse<WxLoginResponse> wxLogin(@Valid @RequestBody WxLoginRequest request) {
        return ApiResponse.success(userAuthService.wxLogin(request.getCode()));
    }

    @Operation(summary = "小程序端绑定手机号（新用户注册或匹配已有账户合并openid）")
    @PostMapping("/wx-bind-phone")
    public ApiResponse<LoginResponse> wxBindPhone(@Valid @RequestBody WxBindPhoneRequest request) {
        return ApiResponse.success(userAuthService.wxBindPhone(request));
    }

    @Operation(summary = "小程序端第一步校验手机号密码")
    @PostMapping("/wx-prepare-bind-phone")
    public ApiResponse<Map<String, Object>> prepareWxBindPhone(@Valid @RequestBody WxBindPhoneRequest request) {
        // login == null 表示当前手机号未匹配到已有账户，前端应进入第二步补充资料，
        // 后续再调用 /wx-bind-phone 正式创建账户。
        LoginResponse login = userAuthService.prepareWxBindPhone(request);
        Map<String, Object> data = new HashMap<>();
        data.put("completed", login != null);
        data.put("login", login);
        return ApiResponse.success(data);
    }

    @Operation(summary = "Web端注册")
    @PostMapping("/web-register")
    public ApiResponse<LoginResponse> webRegister(@Valid @RequestBody WebRegisterRequest request) {
        return ApiResponse.success(userAuthService.webRegister(request));
    }

    @Operation(summary = "Web端第一步校验手机号是否可注册")
    @PostMapping("/web-prepare-register")
    public ApiResponse<Void> prepareWebRegister(@Valid @RequestBody WebRegisterRequest request) {
        userAuthService.prepareWebRegister(request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "Web端登录")
    @PostMapping("/web-login")
    public ApiResponse<LoginResponse> webLogin(@Valid @RequestBody WebLoginRequest request) {
        return ApiResponse.success(userAuthService.webLogin(request));
    }

    @Deprecated
    @Operation(summary = "获取当前用户信息（兼容路由，主入口请使用 /api/v1/user/info）")
    @GetMapping("/user-info")
    public ApiResponse<UserInfoResponse> getUserInfo() {
        Long userId = UserContext.getUserId();
        return ApiResponse.success(userAccountService.getUserInfo(userId));
    }

    @Deprecated
    @Operation(summary = "更新用户信息（兼容路由，主入口请使用 /api/v1/user/info）")
    @PutMapping("/user-info")
    public ApiResponse<Void> updateUserInfo(@Valid @RequestBody UpdateUserInfoRequest request) {
        Long userId = UserContext.getUserId();
        userAccountService.updateUserInfo(userId, request);
        return ApiResponse.success(null);
    }

    @Deprecated
    @Operation(summary = "修改密码（兼容路由，主入口请使用 /api/v1/user/password）")
    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = UserContext.getUserId();
        userAccountService.changePassword(userId, request);
        return ApiResponse.success(null);
    }

    @Deprecated
    @Operation(summary = "设置偏好标签（兼容路由，主入口请使用 /api/v1/user/preferences）")
    @PostMapping("/preferences")
    public ApiResponse<Void> setPreferences(@Valid @RequestBody PreferencesRequest request) {
        Long userId = UserContext.getUserId();
        userAccountService.setPreferences(userId, request.getCategoryIds());
        return ApiResponse.success(null);
    }
}
