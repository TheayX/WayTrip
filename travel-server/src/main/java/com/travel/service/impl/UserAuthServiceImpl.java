package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.dto.auth.response.LoginResponse;
import com.travel.dto.auth.request.WebLoginRequest;
import com.travel.dto.auth.request.WebRegisterRequest;
import com.travel.dto.auth.request.WxBindPhoneRequest;
import com.travel.dto.auth.response.WxLoginResponse;
import com.travel.entity.User;
import com.travel.mapper.UserMapper;
import com.travel.service.UserAuthService;
import com.travel.util.security.JwtUtils;
import com.travel.util.wechat.WxApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 用户认证服务实现，负责微信与 Web 登录、注册及账号绑定流程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthServiceImpl implements UserAuthService {

    // 默认资料配置
    private static final String DEFAULT_AVATAR_URL = "/uploads/avatar/avatar.jpg";

    // 持久层与外部能力依赖
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final WxApiClient wxApiClient;
    private final BCryptPasswordEncoder passwordEncoder;

    // 微信登录与绑定流程

    @Override
    @Transactional
    public WxLoginResponse wxLogin(String code) {
        String openid = wxApiClient.getOpenid(code);
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

        String token = jwtUtils.generateUserToken(user.getId());

        return WxLoginResponse.builder()
                .isNewUser(false)
                .token(token)
                .expiresIn(jwtUtils.getExpirationSeconds())
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

    // Web 端认证流程

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

        String token = jwtUtils.generateUserToken(user.getId());

        return LoginResponse.builder()
                .token(token)
                .expiresIn(jwtUtils.getExpirationSeconds())
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
    public void prepareWebRegister(WebRegisterRequest request) {
        User existUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getPhone, request.getPhone())
                        .eq(User::getIsDeleted, 0)
        );
        if (existUser != null) {
            throw new BusinessException(ResultCode.PHONE_ALREADY_REGISTERED);
        }
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

        String token = jwtUtils.generateUserToken(user.getId());

        return LoginResponse.builder()
                .token(token)
                .expiresIn(jwtUtils.getExpirationSeconds())
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
    @Transactional
    public LoginResponse wxBindPhone(WxBindPhoneRequest request) {
        String openid = request.getOpenid();

        User existByOpenid = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getOpenid, openid)
                        .eq(User::getIsDeleted, 0)
        );
        if (existByOpenid != null) {
            return buildExistingUserLoginResponse(existByOpenid, false);
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

        String token = jwtUtils.generateUserToken(newUser.getId());

        return LoginResponse.builder()
                .token(token)
                .expiresIn(jwtUtils.getExpirationSeconds())
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
            return buildExistingUserLoginResponse(existByOpenid, false);
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

    // 账号合并与复用

    private LoginResponse mergeExistingWxUser(User existUser, String openid, String password, String phone) {
        if (!StringUtils.hasText(existUser.getPassword())) {
            throw new BusinessException(ResultCode.WEB_LOGIN_FAILED);
        }
        if (!passwordEncoder.matches(password, existUser.getPassword())) {
            throw new BusinessException(ResultCode.WEB_LOGIN_FAILED);
        }

        // 已绑定其他 openid 的账号不允许被当前微信号直接接管。
        if (StringUtils.hasText(existUser.getOpenid()) && !existUser.getOpenid().equals(openid)) {
            throw new BusinessException(ResultCode.PHONE_ALREADY_REGISTERED);
        }

        existUser.setOpenid(openid);
        existUser.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(existUser);
        log.info("微信openid合并到已有账户: userId={}, phone={}", existUser.getId(), phone);

        return buildExistingUserLoginResponse(existUser, true);
    }

    /**
     * 微信绑定流程里如果账号已存在，则统一返回已登录用户响应，避免多处分支重复组装。
     */
    private LoginResponse buildExistingUserLoginResponse(User user, boolean merged) {
        String token = jwtUtils.generateUserToken(user.getId());
        return LoginResponse.builder()
            .token(token)
            .expiresIn(jwtUtils.getExpirationSeconds())
            .user(LoginResponse.UserInfo.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .avatar(user.getAvatarUrl())
                .phone(user.getPhone())
                .isNewUser(false)
                .isMerged(merged)
                .build())
            .build();
    }
}
