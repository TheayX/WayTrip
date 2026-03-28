package com.travel.service.impl;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.dto.auth.AdminLoginRequest;
import com.travel.dto.auth.ChangePasswordRequest;
import com.travel.dto.auth.LoginResponse;
import com.travel.dto.auth.UpdateUserInfoRequest;
import com.travel.dto.auth.UserInfoResponse;
import com.travel.dto.auth.WebLoginRequest;
import com.travel.dto.auth.WebRegisterRequest;
import com.travel.dto.auth.WxLoginResponse;
import com.travel.entity.Admin;
import com.travel.entity.SpotCategory;
import com.travel.entity.User;
import com.travel.entity.UserPreference;
import com.travel.mapper.AdminMapper;
import com.travel.mapper.SpotCategoryMapper;
import com.travel.mapper.UserMapper;
import com.travel.mapper.UserPreferenceMapper;
import com.travel.service.RecommendationService;
import com.travel.util.JwtUtil;
import com.travel.util.WxApiUtil;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "test");
        assistant.setCurrentNamespace("test");
        TableInfoHelper.initTableInfo(assistant, User.class);
        TableInfoHelper.initTableInfo(assistant, Admin.class);
        TableInfoHelper.initTableInfo(assistant, UserPreference.class);
        TableInfoHelper.initTableInfo(assistant, SpotCategory.class);
    }

    @Mock
    private UserMapper userMapper;

    @Mock
    private AdminMapper adminMapper;

    @Mock
    private UserPreferenceMapper userPreferenceMapper;

    @Mock
    private SpotCategoryMapper spotCategoryMapper;

    @Mock
    private RecommendationService recommendationService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private WxApiUtil wxApiUtil;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void wxLogin_blankOpenid_throws() {
        when(wxApiUtil.getOpenid("code")).thenReturn("");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.wxLogin("code"));

        assertEquals(ResultCode.WX_LOGIN_FAILED.getCode(), ex.getCode());
    }

    @Test
    void wxLogin_newUser_returnsOpenidOnly_andDoesNotCreateUser() {
        when(wxApiUtil.getOpenid("code")).thenReturn("openid-1");
        when(userMapper.selectOne(any())).thenReturn(null);

        WxLoginResponse res = authService.wxLogin("code");

        assertTrue(res.getIsNewUser());
        assertEquals("openid-1", res.getOpenid());
        assertNull(res.getToken());
        assertNull(res.getUser());
        verify(userMapper, never()).insert(any());
        verify(userMapper, never()).updateById(any());
    }

    @Test
    void wxLogin_existingUser_returnsToken_andMarksNotNew() {
        User user = new User();
        user.setId(1L);
        user.setNickname("n");
        user.setAvatarUrl("/a.png");
        user.setPhone("13800138000");
        user.setIsDeleted(0);

        when(wxApiUtil.getOpenid("code")).thenReturn("openid-1");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(jwtUtil.generateUserToken(1L)).thenReturn("token-u");
        when(jwtUtil.getExpirationSeconds()).thenReturn(123L);

        WxLoginResponse res = authService.wxLogin("code");

        assertFalse(res.getIsNewUser());
        assertEquals("token-u", res.getToken());
        assertEquals(123L, res.getExpiresIn());
        assertNotNull(res.getUser());
        assertEquals(1L, res.getUser().getId());
        assertEquals("n", res.getUser().getNickname());
        assertEquals("/a.png", res.getUser().getAvatar());
        assertEquals("13800138000", res.getUser().getPhone());
        verify(userMapper).update(eq(null), any());
    }

    @Test
    void wxLogin_deletedUser_setsIsReactivatedTrue() {
        User user = new User();
        user.setId(1L);
        user.setNickname("n");
        user.setAvatarUrl("/a.png");
        user.setPhone("13800138000");
        user.setIsDeleted(1);

        when(wxApiUtil.getOpenid("code")).thenReturn("openid-1");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(jwtUtil.generateUserToken(1L)).thenReturn("token-u");
        when(jwtUtil.getExpirationSeconds()).thenReturn(123L);

        WxLoginResponse res = authService.wxLogin("code");

        assertTrue(res.getIsReactivated());
        verify(userMapper).update(eq(null), any());
    }

    @Test
    void getUserInfo_returnsPreferences() {
        User user = new User();
        user.setId(1L);
        user.setNickname("n");
        user.setAvatarUrl("/a.png");
        user.setPhone("13800138000");
        user.setPassword("bcrypt");
        user.setIsDeleted(0);

        UserPreference preference = new UserPreference();
        preference.setUserId(1L);
        preference.setTag("1");
        preference.setIsDeleted(0);

        SpotCategory category = new SpotCategory();
        category.setId(1L);
        category.setName("自然风光");
        category.setIsDeleted(0);

        when(userMapper.selectById(1L)).thenReturn(user);
        when(userPreferenceMapper.selectList(any())).thenReturn(List.of(preference));
        when(spotCategoryMapper.selectList(any())).thenReturn(List.of(category));

        UserInfoResponse res = authService.getUserInfo(1L);

        assertEquals(1L, res.getId());
        assertEquals("n", res.getNickname());
        assertEquals(List.of("自然风光"), res.getPreferences());
        assertEquals(List.of(1L), res.getPreferenceCategoryIds());
        assertEquals(List.of("自然风光"), res.getPreferenceCategoryNames());
        assertTrue(res.getHasPassword());
    }

    @Test
    void updateUserInfo_trimsPhone_andUpdatesUser() {
        User user = new User();
        user.setId(1L);
        user.setNickname("old");
        user.setAvatarUrl("/old.png");
        user.setPhone("13800138000");
        user.setIsDeleted(0);

        when(userMapper.selectById(1L)).thenReturn(user);

        UpdateUserInfoRequest req = new UpdateUserInfoRequest();
        req.setNickname("newNick");
        req.setAvatar("/new.png");
        req.setPhone(" 13800138000 ");

        authService.updateUserInfo(1L, req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        User updated = captor.getValue();
        assertEquals("newNick", updated.getNickname());
        assertEquals("/new.png", updated.getAvatarUrl());
        assertEquals("13800138000", updated.getPhone());
    }

    @Test
    void changePassword_requiresOldPasswordWhenAlreadySet() {
        User user = new User();
        user.setId(1L);
        user.setPassword("bcrypt");
        user.setIsDeleted(0);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.matches("old", "bcrypt")).thenReturn(false);

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("old");
        req.setNewPassword("new");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.changePassword(1L, req));
        assertEquals(ResultCode.OLD_PASSWORD_ERROR.getCode(), ex.getCode());
    }

    @Test
    void changePassword_allowsSettingWhenNoExistingPassword() {
        User user = new User();
        user.setId(1L);
        user.setPassword(null);
        user.setIsDeleted(0);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.encode("new")).thenReturn("bcrypt-new");

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setNewPassword("new");

        authService.changePassword(1L, req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals("bcrypt-new", captor.getValue().getPassword());
    }

    @Test
    void deactivateAccount_setsIsDeleted() {
        User user = new User();
        user.setId(1L);
        user.setIsDeleted(0);
        when(userMapper.selectById(1L)).thenReturn(user);

        authService.deactivateAccount(1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getIsDeleted());
    }

    @Test
    void setPreferences_replacesOldAndInsertsNew() {
        SpotCategory c1 = new SpotCategory();
        c1.setId(1L);
        SpotCategory c2 = new SpotCategory();
        c2.setId(2L);
        when(spotCategoryMapper.selectBatchIds(any())).thenReturn(List.of(c1, c2));
        when(userPreferenceMapper.selectList(any())).thenReturn(List.of());

        authService.setPreferences(1L, List.of(1L, 2L));

        verify(userPreferenceMapper).update(any(UserPreference.class), any());
        verify(userPreferenceMapper, times(2)).insert(any(UserPreference.class));
        verify(recommendationService).invalidateUserRecommendationCache(1L);
    }

    @Test
    void setPreferences_restoresExistingPreferenceWithoutDuplicateInsert() {
        SpotCategory c1 = new SpotCategory();
        c1.setId(1L);
        SpotCategory c2 = new SpotCategory();
        c2.setId(2L);
        when(spotCategoryMapper.selectBatchIds(any())).thenReturn(List.of(c1, c2));

        UserPreference existing = new UserPreference();
        existing.setId(10L);
        existing.setUserId(1L);
        existing.setTag("1");
        existing.setIsDeleted(1);
        when(userPreferenceMapper.selectList(any())).thenReturn(List.of(existing));

        authService.setPreferences(1L, List.of(1L, 2L));

        verify(userPreferenceMapper).update(any(UserPreference.class), any());
        verify(userPreferenceMapper).updateById(any(UserPreference.class));
        verify(userPreferenceMapper, times(1)).insert(any(UserPreference.class));
        verify(recommendationService).invalidateUserRecommendationCache(1L);
    }

    @Test
    void setPreferences_allowsEmptySelection_andOnlyClearsPreferences() {
        authService.setPreferences(1L, List.of());

        verify(userPreferenceMapper).update(any(UserPreference.class), any());
        verify(userPreferenceMapper, never()).insert(any(UserPreference.class));
        verify(userPreferenceMapper, never()).updateById(any(UserPreference.class));
        verify(recommendationService).invalidateUserRecommendationCache(1L);
    }

    @Test
    void adminLogin_disabledAdmin_rejected() {
        Admin admin = new Admin();
        admin.setId(9L);
        admin.setUsername("admin");
        admin.setPassword("bcrypt");
        admin.setIsEnabled(0);
        admin.setIsDeleted(0);
        when(adminMapper.selectOne(any())).thenReturn(admin);

        AdminLoginRequest req = new AdminLoginRequest();
        req.setUsername("admin");
        req.setPassword("pw");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.adminLogin(req));
        assertEquals(ResultCode.ADMIN_DISABLED.getCode(), ex.getCode());
    }

    @Test
    void webRegister_existingPhone_rejected() {
        User exist = new User();
        exist.setId(1L);
        exist.setIsDeleted(0);
        when(userMapper.selectOne(any())).thenReturn(exist);

        WebRegisterRequest req = new WebRegisterRequest();
        req.setPhone("13800138000");
        req.setPassword("pw");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.webRegister(req));
        assertEquals(ResultCode.PHONE_ALREADY_REGISTERED.getCode(), ex.getCode());
    }

    @Test
    void webLogin_reactivatesDeletedAccount_setsFlag() {
        User user = new User();
        user.setId(1L);
        user.setPhone("13800138000");
        user.setPassword("bcrypt");
        user.setIsDeleted(1);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("pw", "bcrypt")).thenReturn(true);
        when(jwtUtil.generateUserToken(1L)).thenReturn("token-u");
        when(jwtUtil.getExpirationSeconds()).thenReturn(123L);

        WebLoginRequest req = new WebLoginRequest();
        req.setPhone("13800138000");
        req.setPassword("pw");

        LoginResponse res = authService.webLogin(req);

        assertNotNull(res.getUser());
        assertTrue(res.getUser().getIsReactivated());
        verify(userMapper).update(eq(null), any());
    }
}
