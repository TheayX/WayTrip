package com.travel.service;

import com.travel.dto.admin.AdminCreateRequest;
import com.travel.dto.admin.AdminListRequest;
import com.travel.dto.admin.AdminListResponse;
import com.travel.dto.admin.AdminResetPasswordRequest;
import com.travel.dto.admin.AdminUpdateRequest;

public interface AdminService {

    AdminListResponse getAdminList(AdminListRequest request);

    Long createAdmin(AdminCreateRequest request);

    void updateAdmin(Long id, AdminUpdateRequest request, Long currentAdminId);

    void resetPassword(Long id, AdminResetPasswordRequest request);

    void deleteAdmin(Long id, Long currentAdminId);
}
