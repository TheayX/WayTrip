package com.travel.controller.admin;

import com.travel.common.result.ApiResponse;
import com.travel.dto.admin.AdminCreateRequest;
import com.travel.dto.admin.AdminListRequest;
import com.travel.dto.admin.AdminListResponse;
import com.travel.dto.admin.AdminResetPasswordRequest;
import com.travel.dto.admin.AdminUpdateRequest;
import com.travel.service.AdminService;
import com.travel.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "管理员管理")
@RestController
@RequestMapping("/api/admin/v1/admins")
@RequiredArgsConstructor
public class AdminManagerController {

    private final AdminService adminService;

    @Operation(summary = "获取管理员列表")
    @GetMapping
    public ApiResponse<AdminListResponse> getAdminList(AdminListRequest request) {
        return ApiResponse.success(adminService.getAdminList(request));
    }

    @Operation(summary = "创建管理员")
    @PostMapping
    public ApiResponse<Map<String, Long>> createAdmin(@Valid @RequestBody AdminCreateRequest request) {
        Long id = adminService.createAdmin(request);
        return ApiResponse.success(Map.of("id", id));
    }

    @Operation(summary = "更新管理员信息")
    @PutMapping("/{id}")
    public ApiResponse<Void> updateAdmin(@PathVariable("id") Long id, @Valid @RequestBody AdminUpdateRequest request) {
        adminService.updateAdmin(id, request, UserContext.getAdminId());
        return ApiResponse.success();
    }

    @Operation(summary = "重置管理员密码")
    @PutMapping("/{id}/password")
    public ApiResponse<Void> resetPassword(@PathVariable("id") Long id, @Valid @RequestBody AdminResetPasswordRequest request) {
        adminService.resetPassword(id, request);
        return ApiResponse.success();
    }

    @Operation(summary = "删除管理员")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAdmin(@PathVariable("id") Long id) {
        adminService.deleteAdmin(id, UserContext.getAdminId());
        return ApiResponse.success();
    }
}
