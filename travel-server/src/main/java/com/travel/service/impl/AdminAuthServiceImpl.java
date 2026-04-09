package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.dto.auth.request.AdminLoginRequest;
import com.travel.dto.auth.response.AdminLoginResponse;
import com.travel.entity.Admin;
import com.travel.mapper.AdminMapper;
import com.travel.service.AdminAuthService;
import com.travel.util.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 管理员认证服务实现，负责后台登录校验与管理员信息查询。
 * <p>
 * 后台登录链路独立实现，便于和普通用户认证分开维护不同的账号状态和令牌策略。
 */
@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    // 持久层与安全依赖

    private final AdminMapper adminMapper;
    private final JwtUtils jwtUtils;
    private final BCryptPasswordEncoder passwordEncoder;

    // 管理员认证流程

    @Override
    public AdminLoginResponse adminLogin(AdminLoginRequest request) {
        Admin admin = findActiveAdminByUsername(request.getUsername());
        if (admin == null) {
            throw new BusinessException(ResultCode.ADMIN_LOGIN_FAILED);
        }

        if (admin.getIsEnabled() == null || admin.getIsEnabled() == 0) {
            throw new BusinessException(ResultCode.ADMIN_DISABLED);
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new BusinessException(ResultCode.ADMIN_LOGIN_FAILED);
        }

        // 登录成功后立即刷新最后登录时间，便于后台审计和账号活跃度统计。
        adminMapper.update(
                null,
                new LambdaUpdateWrapper<Admin>()
                        .eq(Admin::getId, admin.getId())
                        .set(Admin::getLastLoginAt, LocalDateTime.now()));

        String token = jwtUtils.generateAdminToken(admin.getId());

        return AdminLoginResponse.builder()
                .token(token)
                .expiresIn(jwtUtils.getAdminExpirationSeconds())
                .admin(buildAdminInfo(admin))
                .build();
    }

    @Override
    public AdminLoginResponse.AdminInfo getAdminInfo(Long adminId) {
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null || admin.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        return buildAdminInfo(admin);
    }

    /**
     * 登录时只允许命中未删除管理员账号，避免后续鉴权分支重复拼装查询条件。
     */
    private Admin findActiveAdminByUsername(String username) {
        return adminMapper.selectOne(
            new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, username)
                .eq(Admin::getIsDeleted, 0)
        );
    }

    private AdminLoginResponse.AdminInfo buildAdminInfo(Admin admin) {
        // 控制返回字段范围，避免把密码、状态等后台内部字段透出到登录响应。
        return AdminLoginResponse.AdminInfo.builder()
            .id(admin.getId())
            .username(admin.getUsername())
            .realName(admin.getRealName())
            .build();
    }
}
