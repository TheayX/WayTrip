package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.dto.auth.AdminLoginRequest;
import com.travel.dto.auth.AdminLoginResponse;
import com.travel.dto.auth.ChangePasswordRequest;
import com.travel.dto.auth.LoginResponse;
import com.travel.dto.auth.UpdateUserInfoRequest;
import com.travel.dto.auth.UserInfoResponse;
import com.travel.dto.auth.WebLoginRequest;
import com.travel.dto.auth.WebRegisterRequest;
import com.travel.dto.auth.WxBindPhoneRequest;
import com.travel.dto.auth.WxLoginResponse;
import com.travel.entity.Admin;
import com.travel.entity.SpotCategory;
import com.travel.entity.User;
import com.travel.entity.UserPreference;
import com.travel.mapper.AdminMapper;
import com.travel.mapper.SpotCategoryMapper;
import com.travel.mapper.UserMapper;
import com.travel.mapper.UserPreferenceMapper;
import com.travel.service.AuthService;
import com.travel.service.RecommendationService;
import com.travel.util.JwtUtil;
import com.travel.util.WxApiUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_AVATAR_URL = "/uploads/images/avatar.jpg";

    private final UserMapper userMapper;
    private final AdminMapper adminMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final SpotCategoryMapper spotCategoryMapper;
    private final RecommendationService recommendationService;
    private final JwtUtil jwtUtil;
    private final WxApiUtil wxApiUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public WxLoginResponse wxLogin(String code) {
        String openid = wxApiUtil.getOpenid(code);
        if (!StringUtils.hasText(openid)) {
            throw new BusinessException(ResultCode.WX_LOGIN_FAILED);
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getOpenid, openid)
        );

        if (user == null) {
            log.info("微信新用户，等待绑定手机号: openid={}", openid);
            return WxLoginResponse.builder()
                    .isNewUser(true)
                    .openid(openid)
                    .build();
        }

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

        List<UserPreference> preferences = userPreferenceMapper.selectList(
                new LambdaQueryWrapper<UserPreference>()
                        .eq(UserPreference::getUserId, userId)
                        .eq(UserPreference::getIsDeleted, 0)
        );
        Map<Long, String> categoryMap = getCategoryMap();
        List<Long> categoryIds = resolvePreferenceCategoryIds(preferences);
        List<String> categoryNames = categoryIds.stream()
                .map(categoryMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return UserInfoResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .avatar(user.getAvatarUrl())
                .phone(user.getPhone())
                .hasPassword(StringUtils.hasText(user.getPassword()))
                .preferences(categoryNames)
                .preferenceCategoryIds(categoryIds)
                .preferenceCategoryNames(categoryNames)
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

        if (StringUtils.hasText(user.getPassword())) {
            if (!StringUtils.hasText(request.getOldPassword())) {
                throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
            }
            if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
            }
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }

    @Override
    public void deactivateAccount(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        user.setIsDeleted(1);
        userMapper.updateById(user);

        log.info("用户账户已注销: userId={}", userId);
    }

    @Override
    @Transactional
    public void setPreferences(Long userId, List<Long> categoryIds) {
        validateCategoryIds(categoryIds);
        Set<Long> distinctCategoryIds = new LinkedHashSet<>(categoryIds);

        UserPreference deletedPreference = new UserPreference();
        deletedPreference.setIsDeleted(1);
        userPreferenceMapper.update(
                deletedPreference,
                new LambdaUpdateWrapper<UserPreference>().eq(UserPreference::getUserId, userId)
        );

        Map<String, UserPreference> existingPreferenceMap = userPreferenceMapper.selectList(
                new LambdaQueryWrapper<UserPreference>()
                        .eq(UserPreference::getUserId, userId)
                        .in(UserPreference::getTag, distinctCategoryIds.stream().map(String::valueOf).collect(Collectors.toSet()))
        ).stream().collect(Collectors.toMap(UserPreference::getTag, preference -> preference, (left, right) -> left));

        for (Long categoryId : distinctCategoryIds) {
            String tag = String.valueOf(categoryId);
            UserPreference existingPreference = existingPreferenceMap.get(tag);
            if (existingPreference != null) {
                UserPreference restoredPreference = new UserPreference();
                restoredPreference.setId(existingPreference.getId());
                restoredPreference.setIsDeleted(0);
                userPreferenceMapper.updateById(restoredPreference);
                continue;
            }

            UserPreference preference = new UserPreference();
            preference.setUserId(userId);
            preference.setTag(tag);
            preference.setIsDeleted(0);
            userPreferenceMapper.insert(preference);
        }

        recommendationService.invalidateUserRecommendationCache(userId);
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
    @Transactional
    public LoginResponse webRegister(WebRegisterRequest request) {
        User existUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getPhone, request.getPhone())
                        .eq(User::getIsDeleted, 0)
        );
        if (existUser != null) {
            throw new BusinessException(ResultCode.PHONE_ALREADY_REGISTERED);
        }

        User user = new User();
        String nickname = StringUtils.hasText(request.getNickname()) ? request.getNickname() : "web用户";
        user.setNickname(nickname);
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAvatarUrl(DEFAULT_AVATAR_URL);
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.insert(user);
        log.info("Web新用户注册: userId={}, phone={}", user.getId(), request.getPhone());

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
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getPhone, request.getPhone())
        );
        if (user == null || !StringUtils.hasText(user.getPassword())) {
            throw new BusinessException(ResultCode.WEB_LOGIN_FAILED);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.WEB_LOGIN_FAILED);
        }

        boolean isReactivated = false;
        if (user.getIsDeleted() == 1) {
            user.setIsDeleted(0);
            isReactivated = true;
            log.info("账户已恢复: userId={}", user.getId());
        }

        userMapper.update(null,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, user.getId())
                        .set(User::getLastLoginAt, LocalDateTime.now())
                        .set(User::getIsDeleted, user.getIsDeleted()));

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

        User existByOpenid = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getOpenid, openid)
                        .eq(User::getIsDeleted, 0)
        );
        if (existByOpenid != null) {
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

        User existUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getPhone, request.getPhone())
                        .eq(User::getIsDeleted, 0)
        );

        if (existUser != null) {
            return mergeExistingWxUser(existUser, openid, request.getPassword(), request.getPhone());
        }

        User newUser = new User();
        newUser.setOpenid(openid);
        newUser.setPhone(request.getPhone());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setNickname("微信用户");
        newUser.setAvatarUrl(DEFAULT_AVATAR_URL);
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

    @Override
    @Transactional
    public LoginResponse prepareWxBindPhone(WxBindPhoneRequest request) {
        User existByOpenid = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getOpenid, request.getOpenid())
                        .eq(User::getIsDeleted, 0)
        );
        if (existByOpenid != null) {
            String token = jwtUtil.generateUserToken(existByOpenid.getId());
            LoginResponse login = LoginResponse.builder()
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
            return login;
        }

        User existUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getPhone, request.getPhone())
                        .eq(User::getIsDeleted, 0)
        );
        if (existUser != null) {
            return mergeExistingWxUser(existUser, request.getOpenid(), request.getPassword(), request.getPhone());
        }

        return null;
    }

    private LoginResponse mergeExistingWxUser(User existUser, String openid, String password, String phone) {
        if (!StringUtils.hasText(existUser.getPassword())) {
            throw new BusinessException(ResultCode.WEB_LOGIN_FAILED);
        }
        if (!passwordEncoder.matches(password, existUser.getPassword())) {
            throw new BusinessException(ResultCode.WEB_LOGIN_FAILED);
        }

        if (StringUtils.hasText(existUser.getOpenid()) && !existUser.getOpenid().equals(openid)) {
            throw new BusinessException(ResultCode.PHONE_ALREADY_REGISTERED);
        }

        existUser.setOpenid(openid);
        existUser.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(existUser);
        log.info("微信openid合并到已有账户: userId={}, phone={}", existUser.getId(), phone);

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
    }

    private void validateCategoryIds(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "偏好分类不能为空");
        }

        Set<Long> distinctIds = new LinkedHashSet<>(categoryIds);
        List<SpotCategory> categories = spotCategoryMapper.selectBatchIds(distinctIds);
        Set<Long> validIds = categories.stream().map(SpotCategory::getId).collect(Collectors.toSet());

        if (validIds.size() != distinctIds.size()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "存在无效的分类ID");
        }
    }

    private Map<Long, String> getCategoryMap() {
        return spotCategoryMapper.selectList(
                new LambdaQueryWrapper<SpotCategory>().eq(SpotCategory::getIsDeleted, 0)
        ).stream().collect(Collectors.toMap(SpotCategory::getId, SpotCategory::getName));
    }

    private List<Long> resolvePreferenceCategoryIds(List<UserPreference> preferences) {
        return preferences.stream()
                .map(UserPreference::getTag)
                .map(this::parsePreferenceCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private Long parsePreferenceCategoryId(String tag) {
        if (!StringUtils.hasText(tag)) {
            return null;
        }

        try {
            return Long.parseLong(tag.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
