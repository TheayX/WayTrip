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
    public WxLoginResponse wxLogin(String code) {
        // 调用微信API获取openid
        String openid = wxApiUtil.getOpenid(code);
        if (!StringUtils.hasText(openid)) {
            throw new BusinessException(ResultCode.WX_LOGIN_FAILED);
        }

        // 查询用户是否存在（通过openid查找）
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getOpenid, openid)
        );

        if (user == null) {
            // 新用户，不自动创建，只返回openid让前端继续填写手机号密码
            log.info("微信新用户，等待绑定手机号: openid={}", openid);
            return WxLoginResponse.builder()
                    .isNewUser(true)
                    .openid(openid)
                    .build();
        }

        // 老用户，正常登录
        boolean isReactivated = false;
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getLastLoginAt, now);
        if (user.getIsDeleted() == 1) {
            updateWrapper.set(User::getIsDeleted, 0);
            isReactivated = true;
            log.info("账户已恢复: userId={}", user.getId());
        }
        userMapper.update(null, updateWrapper);

        // 生成Token
        String token = jwtUtil.generateUserToken(user.getId());

        return WxLoginResponse.builder()
                .isNewUser(false)
                .token(token)
                .expiresIn(jwtUtil.getExpirationSeconds())
                .isReactivated(isReactivated)
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .nickname(user.getNickname())
                        .avatar(user.getAvatarUrl())
                        .phone(user.getPhone())
                        .isNewUser(false)
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
                .avatar(user.getAvatarUrl())
                .phone(user.getPhone())
                .hasPassword(StringUtils.hasText(user.getPassword()))
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
            user.setAvatarUrl(request.getAvatar());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }
        userMapper.updateById(user);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        // 如果用户已设置过密码，需要验证旧密码
        if (StringUtils.hasText(user.getPassword())) {
            if (!StringUtils.hasText(request.getOldPassword())) {
                throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
            }
            if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
            }
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }

    @Override
    public void deactivateAccount(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        // 标记账户为已删除
        user.setIsDeleted(1);
        userMapper.updateById(user);

        log.info("用户账户已注销: userId={}", userId);
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

        if (admin.getIsEnabled() == null || admin.getIsEnabled() == 0) {
            throw new BusinessException(ResultCode.ADMIN_DISABLED);
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new BusinessException(ResultCode.ADMIN_LOGIN_FAILED);
        }

        // 更新最后登录时间，不触发 updatedAt
        adminMapper.update(
                null,
                new LambdaUpdateWrapper<Admin>()
                        .eq(Admin::getId, admin.getId())
                        .set(Admin::getLastLoginAt, LocalDateTime.now()));

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
    @Transactional
    public LoginResponse webRegister(WebRegisterRequest request) {
        // 检查手机号是否已注册
        User existUser = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getPhone, request.getPhone())
                .eq(User::getIsDeleted, 0)
        );
        if (existUser != null) {
            throw new BusinessException(ResultCode.PHONE_ALREADY_REGISTERED);
        }

        // 创建新用户
        User user = new User();
        String nickname = StringUtils.hasText(request.getNickname()) ? request.getNickname() : "web用户";
        user.setNickname(nickname);
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAvatarUrl("");
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.insert(user);
        log.info("Web新用户注册: userId={}, phone={}", user.getId(), request.getPhone());

        // 生成Token
        String token = jwtUtil.generateUserToken(user.getId());

        return LoginResponse.builder()
                .token(token)
                .expiresIn(jwtUtil.getExpirationSeconds())
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .nickname(user.getNickname())
                        .avatar(user.getAvatarUrl())
                        .phone(user.getPhone())
                        .isNewUser(true)
                        .build())
                .build();
    }

    @Override
    public LoginResponse webLogin(WebLoginRequest request) {
        // 根据手机号查找用户（包括已删除的账户）
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getPhone, request.getPhone())
        );
        if (user == null || !StringUtils.hasText(user.getPassword())) {
            throw new BusinessException(ResultCode.WEB_LOGIN_FAILED);
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.WEB_LOGIN_FAILED);
        }

        // 检查账户是否被注销
        boolean isReactivated = false;
        if (user.getIsDeleted() == 1) {
            // 自动恢复被注销的账户
            user.setIsDeleted(0);
            isReactivated = true;
            log.info("账户已恢复: userId={}", user.getId());
        }

        // 更新最后登录时间和状态
        userMapper.update(null,
            new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getLastLoginAt, LocalDateTime.now())
                .set(User::getIsDeleted, user.getIsDeleted()));

        // 生成Token
        String token = jwtUtil.generateUserToken(user.getId());

        return LoginResponse.builder()
                .token(token)
                .expiresIn(jwtUtil.getExpirationSeconds())
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .nickname(user.getNickname())
                        .avatar(user.getAvatarUrl())
                        .phone(user.getPhone())
                        .isNewUser(false)
                        .isReactivated(isReactivated)
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

    @Override
    @Transactional
    public LoginResponse wxBindPhone(WxBindPhoneRequest request) {
        String openid = request.getOpenid();

        // 确认该openid尚未绑定用户（防止重复调用）
        User existByOpenid = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getOpenid, openid)
                .eq(User::getIsDeleted, 0)
        );
        if (existByOpenid != null) {
            // openid已绑定用户，直接返回登录信息
            String token = jwtUtil.generateUserToken(existByOpenid.getId());
            return LoginResponse.builder()
                    .token(token)
                    .expiresIn(jwtUtil.getExpirationSeconds())
                    .user(LoginResponse.UserInfo.builder()
                            .id(existByOpenid.getId())
                            .nickname(existByOpenid.getNickname())
                            .avatar(existByOpenid.getAvatarUrl())
                            .phone(existByOpenid.getPhone())
                            .isNewUser(false)
                            .isMerged(false)
                            .build())
                    .build();
        }

        // 查找手机号对应的已有用户
        User existUser = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getPhone, request.getPhone())
                .eq(User::getIsDeleted, 0)
        );

        if (existUser != null) {
            // 手机号已存在，验证密码
            if (!StringUtils.hasText(existUser.getPassword())) {
                throw new BusinessException(ResultCode.WEB_LOGIN_FAILED);
            }
            if (!passwordEncoder.matches(request.getPassword(), existUser.getPassword())) {
                throw new BusinessException(ResultCode.WEB_LOGIN_FAILED);
            }

            // 密码匹配！把openid合并到已有账户
            if (StringUtils.hasText(existUser.getOpenid()) && !existUser.getOpenid().equals(openid)) {
                throw new BusinessException(ResultCode.PHONE_ALREADY_REGISTERED);
            }
            existUser.setOpenid(openid);
            existUser.setLastLoginAt(LocalDateTime.now());
            userMapper.updateById(existUser);
            log.info("微信openid合并到已有账户: userId={}, phone={}", existUser.getId(), request.getPhone());

            String token = jwtUtil.generateUserToken(existUser.getId());

            return LoginResponse.builder()
                    .token(token)
                    .expiresIn(jwtUtil.getExpirationSeconds())
                    .user(LoginResponse.UserInfo.builder()
                            .id(existUser.getId())
                            .nickname(existUser.getNickname())
                            .avatar(existUser.getAvatarUrl())
                            .phone(existUser.getPhone())
                            .isNewUser(false)
                            .isMerged(true)
                            .build())
                    .build();
        } else {
            // 手机号不存在，创建新用户
            User newUser = new User();
            newUser.setOpenid(openid);
            newUser.setPhone(request.getPhone());
            newUser.setPassword(passwordEncoder.encode(request.getPassword()));
            newUser.setNickname("微信用户");
            newUser.setAvatarUrl("");
            newUser.setLastLoginAt(LocalDateTime.now());
            userMapper.insert(newUser);
            log.info("微信新用户注册: userId={}, phone={}", newUser.getId(), request.getPhone());

            String token = jwtUtil.generateUserToken(newUser.getId());

            return LoginResponse.builder()
                    .token(token)
                    .expiresIn(jwtUtil.getExpirationSeconds())
                    .user(LoginResponse.UserInfo.builder()
                            .id(newUser.getId())
                            .nickname(newUser.getNickname())
                            .avatar(newUser.getAvatarUrl())
                            .phone(newUser.getPhone())
                            .isNewUser(true)
                            .isMerged(false)
                            .build())
                    .build();
        }
    }
}
