package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.dto.admin.AdminCreateRequest;
import com.travel.dto.admin.AdminListRequest;
import com.travel.dto.admin.AdminListResponse;
import com.travel.dto.admin.AdminResetPasswordRequest;
import com.travel.dto.admin.AdminUpdateRequest;
import com.travel.entity.Admin;
import com.travel.mapper.AdminMapper;
import com.travel.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

/**
 * 管理员服务实现，负责管理端管理员查询、维护与密码重置。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    // 持久层与安全依赖

    private final AdminMapper adminMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    // 管理端管理员查询与维护

    @Override
    public AdminListResponse getAdminList(AdminListRequest request) {
        int page = request.getPage() == null || request.getPage() < 1 ? 1 : request.getPage();
        int pageSize = request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize();

        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Admin::getIsDeleted, 0);

        // 关键字统一匹配用户名和姓名，减少管理端筛选维度。
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w.like(Admin::getUsername, request.getKeyword())
                    .or()
                    .like(Admin::getRealName, request.getKeyword()));
        }
        if (request.getStatus() != null) {
            wrapper.eq(Admin::getIsEnabled, request.getStatus());
        }
        wrapper.orderByAsc(Admin::getId);

        Page<Admin> result = adminMapper.selectPage(new Page<>(page, pageSize), wrapper);

        AdminListResponse response = new AdminListResponse();
        response.setList(result.getRecords().stream().map(this::buildItem).collect(Collectors.toList()));
        response.setTotal(result.getTotal());
        response.setPage(page);
        response.setPageSize(pageSize);
        return response;
    }

    @Override
    @Transactional
    public Long createAdmin(AdminCreateRequest request) {
        validateStatus(request.getStatus());

        Admin existed = adminMapper.selectOne(
                new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, request.getUsername())
        );
        if (existed != null && existed.getIsDeleted() == 0) {
            throw new BusinessException(ResultCode.ADMIN_USERNAME_EXISTS);
        }
        // 历史已删除账号仍保留用户名占用，避免后台账号身份混淆。
        if (existed != null && existed.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.ADMIN_USERNAME_EXISTS, "该用户名已被占用（历史账号）");
        }

        Admin admin = new Admin();
        admin.setUsername(request.getUsername().trim());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setRealName(request.getRealName().trim());
        admin.setIsEnabled(request.getStatus());
        admin.setIsDeleted(0);
        adminMapper.insert(admin);
        log.info("管理员创建成功：adminId={}, username={}", admin.getId(), admin.getUsername());
        return admin.getId();
    }

    @Override
    @Transactional
    public void updateAdmin(Long id, AdminUpdateRequest request, Long currentAdminId) {
        validateStatus(request.getStatus());
        Admin admin = getActiveAdmin(id);

        // 禁止当前管理员把自己禁用，避免后台入口被误锁死。
        if (id.equals(currentAdminId) && request.getStatus() == 0) {
            throw new BusinessException(ResultCode.ADMIN_SELF_OPERATION_FORBIDDEN);
        }

        admin.setRealName(request.getRealName().trim());
        admin.setIsEnabled(request.getStatus());
        adminMapper.updateById(admin);
        log.info("管理员信息更新：adminId={}, realName={}, status={}", id, request.getRealName(), request.getStatus());
    }

    @Override
    @Transactional
    public void resetPassword(Long id, AdminResetPasswordRequest request) {
        Admin admin = getActiveAdmin(id);
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        adminMapper.updateById(admin);
        log.info("管理员密码已重置：adminId={}", id);
    }

    @Override
    @Transactional
    public void deleteAdmin(Long id, Long currentAdminId) {
        // 禁止删除自己，避免后台只剩当前会话时出现不可恢复状态。
        if (id.equals(currentAdminId)) {
            throw new BusinessException(ResultCode.ADMIN_SELF_OPERATION_FORBIDDEN);
        }

        Admin admin = getActiveAdmin(id);
        admin.setIsDeleted(1);
        adminMapper.updateById(admin);
        log.info("管理员已删除：adminId={}, username={}", id, admin.getUsername());
    }

    // 内部校验与转换方法

    private Admin getActiveAdmin(Long id) {
        Admin admin = adminMapper.selectById(id);
        if (admin == null || admin.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.ADMIN_NOT_FOUND);
        }
        return admin;
    }

    private AdminListResponse.AdminItem buildItem(Admin admin) {
        AdminListResponse.AdminItem item = new AdminListResponse.AdminItem();
        item.setId(admin.getId());
        item.setUsername(admin.getUsername());
        item.setRealName(admin.getRealName());
        item.setStatus(admin.getIsEnabled());
        item.setLastLoginAt(admin.getLastLoginAt());
        item.setCreatedAt(admin.getCreatedAt());
        item.setUpdatedAt(admin.getUpdatedAt());
        return item;
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "状态值不合法");
        }
    }
}
