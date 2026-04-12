package com.travel.service.impl;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.dto.auth.request.ChangePasswordRequest;
import com.travel.dto.auth.request.UpdateUserInfoRequest;
import com.travel.dto.user.response.UserInfoResponse;
import com.travel.entity.SpotCategory;
import com.travel.entity.User;
import com.travel.entity.UserPreference;
import com.travel.mapper.UserSpotFavoriteMapper;
import com.travel.mapper.SpotCategoryMapper;
import com.travel.mapper.UserMapper;
import com.travel.mapper.UserPreferenceMapper;
import com.travel.service.RecommendationService;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户账户服务测试
 * 重点覆盖资料修改、密码变更、偏好设置和账号注销逻辑。
 */
@ExtendWith(MockitoExtension.class)
class UserAccountServiceImplTest {

    /**
     * 初始化 MyBatis-Plus Lambda 缓存，确保测试中可使用 Lambda 包装器。
     */
    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "test");
        assistant.setCurrentNamespace("test");
        TableInfoHelper.initTableInfo(assistant, User.class);
        TableInfoHelper.initTableInfo(assistant, UserPreference.class);
        TableInfoHelper.initTableInfo(assistant, SpotCategory.class);
    }

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserPreferenceMapper userPreferenceMapper;

    @Mock
    private SpotCategoryMapper spotCategoryMapper;

    @Mock
    private UserSpotFavoriteMapper userSpotFavoriteMapper;

    @Mock
    private RecommendationService recommendationService;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserAccountServiceImpl userAccountService;

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

        UserInfoResponse res = userAccountService.getUserInfo(1L);

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

        userAccountService.updateUserInfo(1L, req);

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

        BusinessException ex = assertThrows(BusinessException.class, () -> userAccountService.changePassword(1L, req));
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

        userAccountService.changePassword(1L, req);

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

        userAccountService.deactivateAccount(1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getIsDeleted());
    }

    @Test
    void deactivateAccountByAdmin_setsIsDeleted() {
        User user = new User();
        user.setId(2L);
        user.setIsDeleted(0);
        when(userMapper.selectById(2L)).thenReturn(user);

        userAccountService.deactivateAccountByAdmin(2L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getIsDeleted());
    }

    @Test
    void setPreferences_replacesOldAndInsertsNew() {
        User user = new User();
        user.setId(1L);
        user.setIsDeleted(0);
        SpotCategory c1 = new SpotCategory();
        c1.setId(1L);
        c1.setIsDeleted(0);
        SpotCategory c2 = new SpotCategory();
        c2.setId(2L);
        c2.setIsDeleted(0);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(spotCategoryMapper.selectList(any())).thenReturn(List.of(c1, c2));
        when(userPreferenceMapper.selectList(any())).thenReturn(List.of());

        userAccountService.setPreferences(1L, List.of(1L, 2L));

        verify(userPreferenceMapper).update(any(UserPreference.class), any());
        verify(userPreferenceMapper, times(2)).insert(any(UserPreference.class));
        verify(recommendationService).invalidateUserRecommendationCache(1L);
    }

    @Test
    void setPreferences_restoresExistingPreferenceWithoutDuplicateInsert() {
        User user = new User();
        user.setId(1L);
        user.setIsDeleted(0);
        SpotCategory c1 = new SpotCategory();
        c1.setId(1L);
        c1.setIsDeleted(0);
        SpotCategory c2 = new SpotCategory();
        c2.setId(2L);
        c2.setIsDeleted(0);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(spotCategoryMapper.selectList(any())).thenReturn(List.of(c1, c2));

        UserPreference existing = new UserPreference();
        existing.setId(10L);
        existing.setUserId(1L);
        existing.setTag("1");
        existing.setIsDeleted(1);
        when(userPreferenceMapper.selectList(any())).thenReturn(List.of(existing));

        userAccountService.setPreferences(1L, List.of(1L, 2L));

        verify(userPreferenceMapper).update(any(UserPreference.class), any());
        verify(userPreferenceMapper).updateById(any(UserPreference.class));
        verify(userPreferenceMapper, times(1)).insert(any(UserPreference.class));
        verify(recommendationService).invalidateUserRecommendationCache(1L);
    }

    @Test
    void setPreferences_allowsEmptySelection_andOnlyClearsPreferences() {
        User user = new User();
        user.setId(1L);
        user.setIsDeleted(0);
        when(userMapper.selectById(1L)).thenReturn(user);

        userAccountService.setPreferences(1L, List.of());

        verify(userPreferenceMapper).update(any(UserPreference.class), any());
        verify(userPreferenceMapper, never()).insert(any(UserPreference.class));
        verify(userPreferenceMapper, never()).updateById(any(UserPreference.class));
        verify(recommendationService).invalidateUserRecommendationCache(1L);
    }

    @Test
    void setPreferences_rejectsDeletedCategory() {
        User user = new User();
        user.setId(1L);
        user.setIsDeleted(0);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(spotCategoryMapper.selectList(any())).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userAccountService.setPreferences(1L, List.of(99L)));

        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
        assertEquals("存在无效的分类ID", ex.getMessage());
    }
}
