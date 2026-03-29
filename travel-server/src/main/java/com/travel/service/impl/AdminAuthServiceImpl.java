package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.dto.auth.AdminLoginRequest;
import com.travel.dto.auth.AdminLoginResponse;
import com.travel.entity.Admin;
import com.travel.mapper.AdminMapper;
import com.travel.service.AdminAuthService;
import com.travel.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 管理员认证服务实现
 */
@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminMapper adminMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public AdminLoginResponse adminLogin(AdminLoginRequest request) {
        Admin admin = adminMapper.selectOne(
                new LambdaQueryWrapper<Admin>()
                        .eq(Admin::getUsername, request.getUsername())
                        .eq(Admin::getIsDeleted, 0)
        );

        if (admin == null) {
            throw new BusinessException(ResultCode.ADMIN_LOGIN_FAILED);
        }

        if (admin.getIsEnabled() == null || admin.getIsEnabled() == 0) {
            throw new BusinessException(ResultCode.ADMIN_DISABLED);
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new BusinessException(ResultCode.ADMIN_LOGIN_FAILED);
        }

        adminMapper.update(
                null,
                new LambdaUpdateWrapper<Admin>()
                        .eq(Admin::getId, admin.getId())
                        .set(Admin::getLastLoginAt, LocalDateTime.now()));

        String token = jwtUtil.generateAdminToken(admin.getId());

        return AdminLoginResponse.builder()
                .token(token)
                .expiresIn(jwtUtil.getAdminExpirationSeconds())
                .admin(AdminLoginResponse.AdminInfo.builder()
                        .id(admin.getId())
                        .username(admin.getUsername())
                        .realName(admin.getRealName())
                        .build())
                .build();
    }

    @Override
    public AdminLoginResponse.AdminInfo getAdminInfo(Long adminId) {
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null || admin.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        return AdminLoginResponse.AdminInfo.builder()
                .id(admin.getId())
                .username(admin.getUsername())
                .realName(admin.getRealName())
                .build();
    }
}
