package com.travel.service.impl;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.result.PageResult;
import com.travel.dto.user.item.AdminUserPreferenceListItem;
import com.travel.dto.user.request.AdminUserPreferenceListRequest;
import com.travel.entity.User;
import com.travel.entity.UserPreference;
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

@ExtendWith(MockitoExtension.class)
class AdminUserInsightServiceImplTest {

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
}

