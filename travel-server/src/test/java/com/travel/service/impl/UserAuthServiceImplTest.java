package com.travel.service.impl;

import com.travel.dto.auth.request.WebLoginRequest;
import com.travel.dto.auth.response.LoginResponse;
import com.travel.entity.User;
import com.travel.mapper.UserMapper;
import com.travel.service.UserAccountService;
import com.travel.util.security.JwtUtils;
import com.travel.util.wechat.WxApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户认证服务测试
 * 重点覆盖登录恢复账号时与账户服务的联动行为。
 */
@ExtendWith(MockitoExtension.class)
class UserAuthServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private WxApiClient wxApiClient;

    @Mock
    private UserAccountService userAccountService;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    private JwtUtils jwtUtils;
    private UserAuthServiceImpl userAuthService;

    /**
     * 构建用户认证服务测试实例，并初始化 JWT 配置。
     */
    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secret", "travel-recommendation-system-jwt-secret-2026");
        ReflectionTestUtils.setField(jwtUtils, "expiration", 604800000L);
        ReflectionTestUtils.setField(jwtUtils, "adminExpiration", 86400000L);
        jwtUtils.init();
        userAuthService = new UserAuthServiceImpl(userMapper, jwtUtils, wxApiClient, userAccountService, passwordEncoder);
    }

    @Test
    void webLogin_reactivatesDeletedUserThroughAccountService() {
        User user = new User();
        user.setId(7L);
        user.setPhone("13800138000");
        user.setNickname("测试用户");
        user.setAvatarUrl("/uploads/avatar/avatar.jpg");
        user.setPassword("bcrypt");
        user.setIsDeleted(1);
        user.setLastLoginAt(LocalDateTime.now().minusDays(1));
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("123456", "bcrypt")).thenReturn(true);

        WebLoginRequest request = new WebLoginRequest();
        request.setPhone("13800138000");
        request.setPassword("123456");

        LoginResponse response = userAuthService.webLogin(request);

        verify(userAccountService).reactivateAccountAfterLogin(7L);
        verify(userMapper).update(any(), any());
        assertTrue(Boolean.TRUE.equals(response.getUser().getIsReactivated()));
    }
}
