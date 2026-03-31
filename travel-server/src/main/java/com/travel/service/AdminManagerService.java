package com.travel.service;

import com.travel.dto.admin.request.AdminCreateRequest;
import com.travel.dto.admin.request.AdminListRequest;
import com.travel.dto.admin.response.AdminListResponse;
import com.travel.dto.admin.request.AdminResetPasswordRequest;
import com.travel.dto.admin.request.AdminUpdateRequest;

/**
 * 管理员管理服务接口。
 * <p>
 * 定义管理员列表查询、创建、更新、删除和密码重置能力。
 */
public interface AdminManagerService {

    /**
     * 分页获取管理员列表。
     *
     * @param request 查询参数
     * @return 管理员列表响应数据
     */
    AdminListResponse getAdminList(AdminListRequest request);

    /**
     * 创建管理员。
     *
     * @param request 创建请求数据
     * @return 新创建的管理员 ID
     */
    Long createAdmin(AdminCreateRequest request);

    /**
     * 更新管理员信息。
     *
     * @param id 管理员 ID
     * @param request 更新请求数据
     * @param currentAdminId 当前登录管理员 ID
     */
    void updateAdmin(Long id, AdminUpdateRequest request, Long currentAdminId);

    /**
     * 重置管理员密码。
     *
     * @param id 管理员 ID
     * @param request 重置密码请求数据
     */
    void resetPassword(Long id, AdminResetPasswordRequest request);

    /**
     * 删除管理员。
     *
     * @param id 要删除的管理员 ID
     * @param currentAdminId 当前登录管理员 ID
     */
    void deleteAdmin(Long id, Long currentAdminId);
}
