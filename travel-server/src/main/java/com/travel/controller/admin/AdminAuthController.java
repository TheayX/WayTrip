package com.travel.controller.admin;

import com.travel.common.result.ApiResponse;
import com.travel.dto.auth.AdminLoginRequest;
import com.travel.dto.auth.AdminLoginResponse;
import com.travel.service.AdminAuthService;
import com.travel.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 管理端认证控制器，负责管理员登录与信息查询接口。
 */
@Tag(name = "管理端-认证", description = "管理员登录、信息获取相关接口")
@RestController
@RequestMapping("/api/admin/v1/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public ApiResponse<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.success(adminAuthService.adminLogin(request));
    }

    @Operation(summary = "获取管理员信息")
    @GetMapping("/info")
    public ApiResponse<AdminLoginResponse.AdminInfo> getInfo() {
        Long adminId = UserContext.getAdminId();
        return ApiResponse.success(adminAuthService.getAdminInfo(adminId));
    }
}
