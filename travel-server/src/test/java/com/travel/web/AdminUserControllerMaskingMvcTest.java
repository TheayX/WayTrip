package com.travel.web;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.controller.admin.AdminUserController;
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
import com.travel.service.impl.UserServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 管理端用户接口脱敏测试
 * 用于校验手机号脱敏和 OpenID 不外露的返回约束。
 */
class AdminUserControllerMaskingMvcTest {

    private MockMvc mockMvc;

    private UserMapper userMapper;
    private OrderMapper orderMapper;
    private UserSpotFavoriteMapper userSpotFavoriteMapper;
    private ReviewMapper reviewMapper;
    private SpotMapper spotMapper;
    private BCryptPasswordEncoder passwordEncoder;

    /**
     * 初始化 MyBatis-Plus Lambda 缓存，避免测试环境下的实体元数据缺失。
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

    /**
     * 构建仅包含管理端用户控制器的 MockMvc 测试环境。
     */
    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        orderMapper = mock(OrderMapper.class);
        userSpotFavoriteMapper = mock(UserSpotFavoriteMapper.class);
        reviewMapper = mock(ReviewMapper.class);
        spotMapper = mock(SpotMapper.class);
        passwordEncoder = mock(BCryptPasswordEncoder.class);

        UserServiceImpl userService = new UserServiceImpl(
                userMapper,
                orderMapper,
                userSpotFavoriteMapper,
                reviewMapper,
                spotMapper,
                passwordEncoder
        );
        AdminUserController controller = new AdminUserController(userService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getUsers_masksPhone_andDoesNotExposeOpenid() throws Exception {
        User user = buildUser();
        Page<User> page = new Page<>(1, 10);
        page.setRecords(List.of(user));
        page.setTotal(1L);

        when(userMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(orderMapper.selectCount(any())).thenReturn(2L);
        when(userSpotFavoriteMapper.selectCount(any())).thenReturn(3L);
        when(reviewMapper.selectCount(any())).thenReturn(4L);

        mockMvc.perform(get("/api/admin/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].phone").value("138****8000"))
                .andExpect(jsonPath("$.data.list[0].openid").doesNotExist());
    }

    @Test
    void getUserDetail_masksPhone_andDoesNotExposeOpenid() throws Exception {
        User user = buildUser();
        user.setPreferences("自然风光");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(orderMapper.selectCount(any())).thenReturn(1L);
        when(userSpotFavoriteMapper.selectCount(any())).thenReturn(1L);
        when(reviewMapper.selectCount(any())).thenReturn(1L);

        Order order = new Order();
        order.setId(11L);
        order.setOrderNo("T202603220001");
        order.setUserId(1L);
        order.setSpotId(100L);
        order.setStatus(OrderStatus.PAID.getCode());
        order.setVisitDate(LocalDate.now().plusDays(1));
        order.setCreatedAt(LocalDateTime.now().minusDays(1));
        order.setUpdatedAt(LocalDateTime.now().minusHours(1));
        order.setIsDeleted(0);
        when(orderMapper.selectList(any())).thenReturn(List.of(order));

        Spot spot = new Spot();
        spot.setId(100L);
        spot.setName("西湖");
        when(spotMapper.selectBatchIds(any())).thenReturn(List.of(spot));

        mockMvc.perform(get("/api/admin/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.phone").value("138****8000"))
                .andExpect(jsonPath("$.data.openid").doesNotExist());
    }

    /**
     * 构造管理端用户查询用的测试用户夹具。
     */
    private User buildUser() {
        User user = new User();
        user.setId(1L);
        user.setOpenid("openid-should-not-expose");
        user.setNickname("用户A");
        user.setPhone("13800138000");
        user.setAvatarUrl("/uploads/images/avatar.jpg");
        user.setIsDeleted(0);
        user.setCreatedAt(LocalDateTime.now().minusDays(10));
        user.setUpdatedAt(LocalDateTime.now().minusDays(1));
        return user;
    }
}

