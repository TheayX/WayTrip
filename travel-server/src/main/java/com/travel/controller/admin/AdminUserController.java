package com.travel.controller.admin;

import com.travel.common.result.ApiResponse;
import com.travel.dto.user.request.AdminUserListRequest;
import com.travel.dto.user.request.ResetUserPasswordRequest;
import com.travel.dto.user.response.AdminUserDetailResponse;
import com.travel.dto.user.response.AdminUserListResponse;
import com.travel.service.UserAccountService;
import com.travel.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 管理端用户控制器，负责用户查询与账号维护接口。
 * <p>
 * 这里聚焦后台用户账号治理，用户行为洞察则拆到独立控制器，避免接口语义混杂。
 */
@Tag(name = "管理端-用户", description = "管理端用户管理相关接口")
@RestController
@RequestMapping("/api/admin/v1/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserProfileService userProfileService;
    private final UserAccountService userAccountService;

    @Operation(summary = "获取用户列表")
    @GetMapping
    public ApiResponse<AdminUserListResponse> getUsers(AdminUserListRequest request) {
        return ApiResponse.success(userProfileService.getAdminUsers(request));
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    public ApiResponse<AdminUserDetailResponse> getUserDetail(@PathVariable("id") Long id) {
        return ApiResponse.success(userProfileService.getAdminUserDetail(id));
    }

    @Operation(summary = "重置用户密码")
    @PutMapping("/{id}/password")
    public ApiResponse<Void> resetUserPassword(@PathVariable("id") Long id,
                                                @Valid @RequestBody ResetUserPasswordRequest request) {
        userProfileService.resetUserPassword(id, request);
        return ApiResponse.success();
    }

    @Operation(summary = "停用用户")
    @DeleteMapping("/{id}/account")
    public ApiResponse<Void> suspendUserAccount(@PathVariable("id") Long id) {
        userAccountService.suspendUserAccountByAdmin(id);
        return ApiResponse.success();
    }

    @Operation(summary = "恢复用户")
    @PutMapping("/{id}/account/restore")
    public ApiResponse<Void> restoreUserAccount(@PathVariable("id") Long id) {
        userAccountService.restoreUserAccountByAdmin(id);
        return ApiResponse.success();
    }
}
