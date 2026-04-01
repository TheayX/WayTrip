package com.travel.controller.admin;

import com.travel.common.result.ApiResponse;
import com.travel.dto.admin.request.AdminCreateRequest;
import com.travel.dto.admin.request.AdminListRequest;
import com.travel.dto.admin.response.AdminListResponse;
import com.travel.dto.admin.request.AdminResetPasswordRequest;
import com.travel.dto.admin.request.AdminUpdateRequest;
import com.travel.service.AdminManagementService;
import com.travel.util.web.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理端管理员控制器，负责管理员管理接口。
 */
@Tag(name = "管理端-管理员", description = "管理员账户管理相关接口")
@RestController
@RequestMapping("/api/admin/v1/admins")
@RequiredArgsConstructor
public class AdminManagementController {

    private final AdminManagementService adminManagementService;

    @Operation(summary = "获取管理员列表")
    @GetMapping
    public ApiResponse<AdminListResponse> getAdminList(AdminListRequest request) {
        return ApiResponse.success(adminManagementService.getAdminList(request));
    }

    @Operation(summary = "创建管理员")
    @PostMapping
    public ApiResponse<Map<String, Long>> createAdmin(@Valid @RequestBody AdminCreateRequest request) {
        Long id = adminManagementService.createAdmin(request);
        return ApiResponse.success(Map.of("id", id));
    }

    @Operation(summary = "更新管理员信息")
    @PutMapping("/{id}")
    public ApiResponse<Void> updateAdmin(@PathVariable("id") Long id, @Valid @RequestBody AdminUpdateRequest request) {
        adminManagementService.updateAdmin(id, request, UserContextHolder.getAdminId());
        return ApiResponse.success();
    }

    @Operation(summary = "重置管理员密码")
    @PutMapping("/{id}/password")
    public ApiResponse<Void> resetPassword(@PathVariable("id") Long id, @Valid @RequestBody AdminResetPasswordRequest request) {
        adminManagementService.resetPassword(id, request);
        return ApiResponse.success();
    }

    @Operation(summary = "删除管理员")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAdmin(@PathVariable("id") Long id) {
        adminManagementService.deleteAdmin(id, UserContextHolder.getAdminId());
        return ApiResponse.success();
    }
}
