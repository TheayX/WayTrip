package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.dto.auth.*;
import com.travel.entity.Admin;
import com.travel.entity.User;
import com.travel.entity.UserPreference;
import com.travel.mapper.AdminMapper;
import com.travel.mapper.UserMapper;
import com.travel.mapper.UserPreferenceMapper;
import com.travel.service.AuthService;
import com.travel.util.JwtUtil;
import com.travel.util.WxApiUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final AdminMapper adminMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final JwtUtil jwtUtil;
    private final WxApiUtil wxApiUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public LoginResponse wxLogin(String code) {
        // 调用微信API获取openid
        String openid = wxApiUtil.getOpenid(code);
        if (!StringUtils.hasText(openid)) {
            throw new BusinessException(ResultCode.WX_LOGIN_FAILED);
        }

        // 查询用户是否存在
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getOpenid, openid)
        );

        boolean isNewUser = false;
        LocalDateTime now = LocalDateTime.now();
        if (user == null) {
            // 新用户，自动注册
            user = new User();
            user.setOpenid(openid);
            user.setNickname("微信用户");
            user.setAvatar("");
            user.setLastLoginAt(now);
            userMapper.insert(user);
            isNewUser = true;
            log.info("新用户注册: userId={}", user.getId());
        } else {
            LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<User>()
                    .eq(User::getId, user.getId())
                    .set(User::getLastLoginAt, now);
            if (user.getIsDeleted() == 1) {
                updateWrapper.set(User::getIsDeleted, 0);
            }
            userMapper.update(null, updateWrapper);
        }

        // 生成Token
        String token = jwtUtil.generateUserToken(user.getId());

        return LoginResponse.builder()
                .token(token)
                .expiresIn(jwtUtil.getExpirationSeconds())
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .nickname(user.getNickname())
                        .avatar(user.getAvatar())
                        .phone(user.getPhone())
                        .isNewUser(isNewUser)
                        .build())
                .build();
    }

    @Override
    public UserInfoResponse getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        // 获取用户偏好标签
        List<UserPreference> preferences = userPreferenceMapper.selectList(
            new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getUserId, userId)
                .eq(UserPreference::getIsDeleted, 0)
        );
        List<String> tags = preferences.stream()
                .map(UserPreference::getTag)
                .collect(Collectors.toList());

        return UserInfoResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .phone(user.getPhone())
                .preferences(tags)
                .build();
    }

    @Override
    public void updateUserInfo(Long userId, UpdateUserInfoRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname());
        }
        if (StringUtils.hasText(request.getAvatar())) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void setPreferences(Long userId, List<String> tags) {
        // 删除旧的偏好
        UserPreference deletedPreference = new UserPreference();
        deletedPreference.setIsDeleted(1);
        userPreferenceMapper.update(
            deletedPreference,
            new LambdaQueryWrapper<UserPreference>().eq(UserPreference::getUserId, userId)
        );

        // 插入新的偏好
        for (String tag : tags) {
            UserPreference preference = new UserPreference();
            preference.setUserId(userId);
            preference.setTag(tag);
            userPreferenceMapper.insert(preference);
        }
    }

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

        if (admin.getStatus() == null || admin.getStatus() == 0) {
            throw new BusinessException(ResultCode.ADMIN_DISABLED);
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new BusinessException(ResultCode.ADMIN_LOGIN_FAILED);
        }

        // 更新最后登录时间
        admin.setLastLoginAt(LocalDateTime.now());
        adminMapper.updateById(admin);

        // 生成Token
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
