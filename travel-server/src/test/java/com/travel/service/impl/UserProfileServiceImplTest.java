package com.travel.service.impl;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.dto.user.response.AdminUserDetailResponse;
import com.travel.dto.user.request.AdminUserListRequest;
import com.travel.dto.user.response.AdminUserListResponse;
import com.travel.dto.user.request.ResetUserPasswordRequest;
import com.travel.entity.Order;
import com.travel.entity.Review;
import com.travel.entity.Spot;
import com.travel.entity.User;
import com.travel.entity.UserSpotFavorite;
import com.travel.enums.OrderStatus;
import com.travel.mapper.OrderMapper;
import com.travel.mapper.ReviewMapper;
import com.travel.mapper.SpotMapper;
import com.travel.mapper.UserMapper;
import com.travel.mapper.UserSpotFavoriteMapper;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户管理服务测试
 * 重点覆盖管理端用户列表、详情聚合和密码重置逻辑。
 */
@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    /**
     * 初始化 MyBatis-Plus Lambda 缓存，避免测试中的实体字段解析失败。
     */
    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "test");
        assistant.setCurrentNamespace("test");
        TableInfoHelper.initTableInfo(assistant, User.class);
        TableInfoHelper.initTableInfo(assistant, Order.class);
        TableInfoHelper.initTableInfo(assistant, UserSpotFavorite.class);
        TableInfoHelper.initTableInfo(assistant, Review.class);
        TableInfoHelper.initTableInfo(assistant, Spot.class);
    }

    @Mock
    private UserMapper userMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private UserSpotFavoriteMapper userSpotFavoriteMapper;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private SpotMapper spotMapper;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserProfileServiceImpl userService;

    @Test
    void getAdminUsers_returnsPagedUsersWithStats() {
        User user = buildUser(1L, "用户A");
        Page<User> page = new Page<>(1, 10);
        page.setRecords(List.of(user));
        page.setTotal(1L);
        when(userMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(orderMapper.selectCount(any())).thenReturn(3L);
        when(userSpotFavoriteMapper.selectCount(any())).thenReturn(2L);
        when(reviewMapper.selectCount(any())).thenReturn(1L);

        AdminUserListRequest request = new AdminUserListRequest();
        request.setPage(1);
        request.setPageSize(10);
        request.setNickname("用户");

        AdminUserListResponse response = userService.getAdminUsers(request);

        assertEquals(1L, response.getTotal());
        assertEquals(1, response.getPage());
        assertEquals(10, response.getPageSize());
        assertNotNull(response.getList());
        assertEquals(1, response.getList().size());
        AdminUserListResponse.UserItem item = response.getList().get(0);
        assertEquals(1L, item.getId());
        assertEquals("用户A", item.getNickname());
        assertEquals("138****8000", item.getPhone());
        assertEquals(3, item.getOrderCount());
        assertEquals(2, item.getFavoriteCount());
        assertEquals(1, item.getRatingCount());
    }

    @Test
    void getAdminUserDetail_returnsAggregatedDetailAndRecentOrders() {
        User user = buildUser(1L, "用户A");
        user.setPreferences("自然风光,历史文化");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(orderMapper.selectCount(any())).thenReturn(5L);
        when(userSpotFavoriteMapper.selectCount(any())).thenReturn(4L);
        when(reviewMapper.selectCount(any())).thenReturn(3L);

        Order order = new Order();
        order.setId(11L);
        order.setOrderNo("T202603220001");
        order.setUserId(1L);
        order.setSpotId(100L);
        order.setQuantity(2);
        order.setVisitDate(LocalDate.now().plusDays(1));
        order.setStatus(OrderStatus.PAID.getCode());
        order.setCreatedAt(LocalDateTime.now().minusDays(1));
        order.setUpdatedAt(LocalDateTime.now().minusHours(1));
        order.setIsDeleted(0);
        when(orderMapper.selectList(any())).thenReturn(List.of(order));

        Spot spot = new Spot();
        spot.setId(100L);
        spot.setName("西湖");
        when(spotMapper.selectBatchIds(any())).thenReturn(List.of(spot));

        AdminUserDetailResponse response = userService.getAdminUserDetail(1L);

        assertEquals(1L, response.getId());
        assertEquals("用户A", response.getNickname());
        assertEquals("138****8000", response.getPhone());
        assertEquals("自然风光,历史文化", response.getPreferences());
        assertEquals(5, response.getOrderCount());
        assertEquals(4, response.getFavoriteCount());
        assertEquals(3, response.getRatingCount());
        assertNotNull(response.getRecentOrders());
        assertEquals(1, response.getRecentOrders().size());
        AdminUserDetailResponse.RecentOrder recentOrder = response.getRecentOrders().get(0);
        assertEquals("T202603220001", recentOrder.getOrderNo());
        assertEquals("西湖", recentOrder.getSpotName());
        assertEquals("paid", recentOrder.getStatus());
    }

    @Test
    void getAdminUserDetail_throwsWhenUserNotFound() {
        when(userMapper.selectById(999L)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.getAdminUserDetail(999L));

        assertEquals("用户不存在", ex.getMessage());
    }

    @Test
    void resetUserPassword_updatesEncodedPassword() {
        User user = buildUser(1L, "用户A");
        user.setPassword("old-hash");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.encode("newPass123")).thenReturn("new-hash");

        ResetUserPasswordRequest request = new ResetUserPasswordRequest();
        request.setNewPassword("newPass123");

        userService.resetUserPassword(1L, request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals("new-hash", captor.getValue().getPassword());
    }

    @Test
    void resetUserPassword_throwsWhenUserNotFound() {
        when(userMapper.selectById(999L)).thenReturn(null);
        ResetUserPasswordRequest request = new ResetUserPasswordRequest();
        request.setNewPassword("newPass123");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.resetUserPassword(999L, request));

        assertEquals("用户不存在", ex.getMessage());
    }

    private User buildUser(Long id, String nickname) {
        User user = new User();
        user.setId(id);
        user.setNickname(nickname);
        user.setAvatarUrl("/uploads/avatar/avatar.jpg");
        user.setPhone("13800138000");
        user.setIsDeleted(0);
        user.setCreatedAt(LocalDateTime.now().minusDays(10));
        user.setUpdatedAt(LocalDateTime.now().minusDays(1));
        return user;
    }
}
