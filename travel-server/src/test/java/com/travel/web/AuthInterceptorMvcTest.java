package com.travel.web;

import com.travel.common.exception.GlobalExceptionHandler;
import com.travel.controller.app.UserAccountController;
import com.travel.dto.auth.AdminLoginResponse;
import com.travel.dto.auth.UserInfoResponse;
import com.travel.dto.user.AdminUserListResponse;
import com.travel.interceptor.AuthInterceptor;
import com.travel.service.AdminAuthService;
import com.travel.service.UserAccountService;
import com.travel.service.UserAuthService;
import com.travel.service.UserService;
import com.travel.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthInterceptorMvcTest {

    private MockMvc mockMvc;
    private JwtUtil jwtUtil;
    private UserAuthService userAuthService;
    private UserAccountService userAccountService;
    private AdminAuthService adminAuthService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "travel-recommendation-system-jwt-secret-2026");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 604800000L);
        ReflectionTestUtils.setField(jwtUtil, "adminExpiration", 86400000L);
        jwtUtil.init();

        AuthInterceptor authInterceptor = new AuthInterceptor();
        ReflectionTestUtils.setField(authInterceptor, "jwtUtil", jwtUtil);

        userAuthService = Mockito.mock(UserAuthService.class);
        userAccountService = Mockito.mock(UserAccountService.class);
        adminAuthService = Mockito.mock(AdminAuthService.class);
        userService = Mockito.mock(UserService.class);
        Mockito.when(userAccountService.getUserInfo(1L)).thenReturn(
                UserInfoResponse.builder()
                        .id(1L)
                        .nickname("测试用户")
                        .avatar("/uploads/images/avatar.jpg")
                        .phone("13800138000")
                        .hasPassword(true)
                        .preferences(List.of("自然风光"))
                        .build()
        );
        Mockito.when(adminAuthService.getAdminInfo(99L)).thenReturn(
                AdminLoginResponse.AdminInfo.builder()
                        .id(99L)
                        .username("admin")
                        .realName("系统管理员")
                        .build()
        );
        AdminUserListResponse listResponse = new AdminUserListResponse();
        listResponse.setList(java.util.List.of(
                new AdminUserListResponse.UserItem(
                        1L,
                        "用户A",
                        "/uploads/images/avatar.jpg",
                        "13800138000",
                        1,
                        2,
                        3,
                        java.time.LocalDateTime.now(),
                        java.time.LocalDateTime.now()
                )
        ));
        listResponse.setTotal(1L);
        listResponse.setPage(1);
        listResponse.setPageSize(10);
        Mockito.when(userService.getAdminUsers(Mockito.any())).thenReturn(listResponse);

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new UserAccountController(userAccountService),
                        new com.travel.controller.app.AuthController(userAuthService, userAccountService),
                        new com.travel.controller.admin.AdminUserController(userService),
                        new com.travel.controller.admin.AdminAuthController(adminAuthService)
                )
                .addInterceptors(authInterceptor)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void protectedUserEndpoint_rejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/user/info").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10002))
                .andExpect(jsonPath("$.message").value("Token无效或过期"));
    }

    @Test
    void protectedUserEndpoint_acceptsUserToken() throws Exception {
        String token = jwtUtil.generateUserToken(1L);

        mockMvc.perform(get("/api/v1/user/info")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.nickname").value("测试用户"));
    }

    @Test
    void protectedUserEndpoint_rejectsAdminToken() throws Exception {
        String token = jwtUtil.generateAdminToken(99L);

        mockMvc.perform(get("/api/v1/user/info")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10002));
    }

    @Test
    void protectedAdminEndpoint_acceptsAdminToken() throws Exception {
        String token = jwtUtil.generateAdminToken(99L);

        mockMvc.perform(get("/api/admin/v1/auth/info")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(99))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    void protectedAuthUserInfo_rejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/user-info").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10002));
    }

    @Test
    void adminUsers_rejectsUserToken() throws Exception {
        String token = jwtUtil.generateUserToken(1L);

        mockMvc.perform(get("/api/admin/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10002));
    }

    @Test
    void adminUsers_acceptsAdminToken_andDoesNotExposeOpenid() throws Exception {
        String token = jwtUtil.generateAdminToken(99L);

        mockMvc.perform(get("/api/admin/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].id").value(1))
                .andExpect(jsonPath("$.data.list[0].openid").doesNotExist());
    }
}
