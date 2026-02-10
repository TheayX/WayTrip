package com.travel.controller.admin;

import com.travel.common.result.ApiResponse;
import com.travel.dto.user.*;
import com.travel.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端用户接口", description = "管理端用户管理相关接口")
@RestController
@RequestMapping("/api/admin/v1/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @Operation(summary = "获取用户列表")
    @GetMapping
    public ApiResponse<AdminUserListResponse> getUsers(AdminUserListRequest request) {
        return ApiResponse.success(userService.getAdminUsers(request));
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    public ApiResponse<AdminUserDetailResponse> getUserDetail(@PathVariable("id") Long id) {
        return ApiResponse.success(userService.getAdminUserDetail(id));
    }
}
