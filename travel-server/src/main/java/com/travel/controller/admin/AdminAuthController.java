package com.travel.controller.admin;

import com.travel.common.result.ApiResponse;
import com.travel.dto.auth.AdminLoginRequest;
import com.travel.dto.auth.AdminLoginResponse;
import com.travel.service.AuthService;
import com.travel.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 管理端认证接口
 */
@Tag(name = "管理员认证")
@RestController
@RequestMapping("/api/admin/v1/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthService authService;

    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public ApiResponse<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.success(authService.adminLogin(request));
    }

    @Operation(summary = "获取管理员信息")
    @GetMapping("/info")
    public ApiResponse<AdminLoginResponse.AdminInfo> getInfo() {
        Long adminId = UserContext.getAdminId();
        return ApiResponse.success(authService.getAdminInfo(adminId));
    }
}
