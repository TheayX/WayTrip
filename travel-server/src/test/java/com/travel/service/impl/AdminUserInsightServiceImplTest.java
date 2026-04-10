package com.travel.service.impl;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.result.PageResult;
import com.travel.dto.user.item.AdminUserFavoriteListItem;
import com.travel.dto.user.item.AdminUserPreferenceListItem;
import com.travel.dto.user.item.AdminUserViewListItem;
import com.travel.dto.user.request.AdminUserFavoriteListRequest;
import com.travel.dto.user.request.AdminUserPreferenceListRequest;
import com.travel.dto.user.request.AdminUserViewListRequest;
import com.travel.entity.User;
import com.travel.entity.UserPreference;
import com.travel.entity.UserSpotFavorite;
import com.travel.entity.UserSpotView;
import com.travel.entity.Spot;
import com.travel.mapper.SpotCategoryMapper;
import com.travel.mapper.SpotMapper;
import com.travel.mapper.UserMapper;
import com.travel.mapper.UserPreferenceMapper;
import com.travel.mapper.UserSpotFavoriteMapper;
import com.travel.mapper.UserSpotViewMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户运营洞察服务测试。
 * <p>
 * 重点覆盖偏好标签解析与列表装配这类容易受脏数据影响的边界场景。
 */
@ExtendWith(MockitoExtension.class)
class AdminUserInsightServiceImplTest {

    /**
     * 初始化 MyBatis-Plus Lambda 缓存，避免测试里使用 LambdaQueryWrapper 时缺少表元信息。
     */
    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "test");
        assistant.setCurrentNamespace("test");
        TableInfoHelper.initTableInfo(assistant, User.class);
        TableInfoHelper.initTableInfo(assistant, UserPreference.class);
    }

    @Mock
    private UserMapper userMapper;

    @Mock
    private SpotMapper spotMapper;

    @Mock
    private SpotCategoryMapper spotCategoryMapper;

    @Mock
    private UserPreferenceMapper userPreferenceMapper;

    @Mock
    private UserSpotFavoriteMapper userSpotFavoriteMapper;

    @Mock
    private UserSpotViewMapper userSpotViewMapper;

    @InjectMocks
    private AdminUserInsightServiceImpl service;

    @Test
    void getPreferenceList_skipsCategoryBatchQueryWhenNoValidCategoryId() {
        User user = new User();
        user.setId(1L);
        user.setNickname("用户A");
        user.setPhone("13800138000");
        user.setCreatedAt(LocalDateTime.now().minusDays(3));
        user.setUpdatedAt(LocalDateTime.now().minusDays(1));

        Page<User> page = new Page<>(1, 10);
        page.setRecords(List.of(user));
        page.setTotal(1L);
        when(userMapper.selectPage(any(Page.class), any())).thenReturn(page);

        UserPreference invalidPreference = new UserPreference();
        invalidPreference.setUserId(1L);
        invalidPreference.setTag("invalid-tag");
        invalidPreference.setIsDeleted(0);
        invalidPreference.setUpdatedAt(LocalDateTime.now());
        when(userPreferenceMapper.selectList(any())).thenReturn(List.of(invalidPreference));

        AdminUserPreferenceListRequest request = new AdminUserPreferenceListRequest();
        request.setPage(1);
        request.setPageSize(10);

        PageResult<AdminUserPreferenceListItem> result = service.getPreferenceList(request);

        assertEquals(1L, result.getTotal());
        assertNotNull(result.getList());
        assertEquals(1, result.getList().size());
        assertNotNull(result.getList().get(0).getPreferenceTags());
        assertEquals(0, result.getList().get(0).getPreferenceTags().size());

        verify(spotCategoryMapper, never()).selectBatchIds(any());
    }

    @Test
    void getFavoriteList_marksDeletedUserAsDeactivated() {
        UserSpotFavorite favorite = new UserSpotFavorite();
        favorite.setId(1L);
        favorite.setUserId(10L);
        favorite.setSpotId(100L);
        favorite.setIsDeleted(0);
        favorite.setCreatedAt(LocalDateTime.now());

        Page<UserSpotFavorite> page = new Page<>(1, 10);
        page.setRecords(List.of(favorite));
        page.setTotal(1L);
        when(userSpotFavoriteMapper.selectPage(any(Page.class), any())).thenReturn(page);

        User deletedUser = new User();
        deletedUser.setId(10L);
        deletedUser.setNickname("原昵称");
        deletedUser.setIsDeleted(1);

        Spot spot = new Spot();
        spot.setId(100L);
        spot.setName("测试景点");

        when(userMapper.selectBatchIds(any())).thenReturn(List.of(deletedUser));
        when(spotMapper.selectBatchIds(any())).thenReturn(List.of(spot));

        AdminUserFavoriteListRequest request = new AdminUserFavoriteListRequest();
        request.setPage(1);
        request.setPageSize(10);

        PageResult<AdminUserFavoriteListItem> result = service.getFavoriteList(request);

        assertEquals("已注销用户", result.getList().get(0).getNickname());
    }

    @Test
    void getViewList_marksMissingUserAsDeactivated() {
        UserSpotView view = new UserSpotView();
        view.setId(1L);
        view.setUserId(20L);
        view.setSpotId(200L);
        view.setViewSource("detail");
        view.setViewDuration(30);
        view.setCreatedAt(LocalDateTime.now());

        Page<UserSpotView> page = new Page<>(1, 10);
        page.setRecords(List.of(view));
        page.setTotal(1L);
        when(userSpotViewMapper.selectPage(any(Page.class), any())).thenReturn(page);

        Spot spot = new Spot();
        spot.setId(200L);
        spot.setName("另一个景点");

        when(userMapper.selectBatchIds(any())).thenReturn(List.of());
        when(spotMapper.selectBatchIds(any())).thenReturn(List.of(spot));

        AdminUserViewListRequest request = new AdminUserViewListRequest();
        request.setPage(1);
        request.setPageSize(10);

        PageResult<AdminUserViewListItem> result = service.getViewList(request);

        assertEquals("已注销用户", result.getList().get(0).getNickname());
    }
}

