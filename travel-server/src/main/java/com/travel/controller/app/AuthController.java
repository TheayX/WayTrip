package com.travel.controller.app;

import com.travel.common.result.ApiResponse;
import com.travel.dto.auth.request.WebLoginRequest;
import com.travel.dto.auth.request.WebRegisterRequest;
import com.travel.dto.auth.request.WxBindPhoneRequest;
import com.travel.dto.auth.request.WxLoginRequest;
import com.travel.dto.auth.response.LoginResponse;
import com.travel.dto.auth.response.WxLoginResponse;
import com.travel.service.UserAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户端认证控制器，负责登录、注册与账号绑定接口。
 */
@Tag(name = "用户端-认证", description = "用户登录、注册相关接口")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserAuthService userAuthService;

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

    /**
     * 预校验小程序绑定流程；若手机号已匹配现有账号，则直接返回登录结果，
     * 否则由前端继续补充资料并调用正式绑定接口。
     */
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
}
