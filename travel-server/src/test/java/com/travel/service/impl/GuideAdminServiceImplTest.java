package com.travel.service.impl;

import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.dto.guide.request.AdminGuideRequest;
import com.travel.common.result.PageResult;
import com.travel.entity.Admin;
import com.travel.entity.Guide;
import com.travel.mapper.AdminMapper;
import com.travel.mapper.GuideMapper;
import com.travel.mapper.GuideSpotRelationMapper;
import com.travel.mapper.SpotMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 攻略管理服务测试
 * 重点覆盖创建攻略前的管理员有效性校验。
 */
@ExtendWith(MockitoExtension.class)
class GuideAdminServiceImplTest {

    @Mock
    private GuideMapper guideMapper;

    @Mock
    private GuideSpotRelationMapper guideSpotRelationMapper;

    @Mock
    private SpotMapper spotMapper;

    @Mock
    private AdminMapper adminMapper;

    @InjectMocks
    private GuideAdminServiceImpl guideAdminService;

    @Test
    void createGuide_rejectsDisabledAdmin() {
        Admin admin = new Admin();
        admin.setId(9L);
        admin.setIsDeleted(0);
        admin.setIsEnabled(0);
        when(adminMapper.selectById(9L)).thenReturn(admin);

        AdminGuideRequest request = new AdminGuideRequest();
        request.setTitle("攻略");
        request.setContent("内容");

        BusinessException ex = assertThrows(BusinessException.class,
            () -> guideAdminService.createGuide(request, 9L));

        assertEquals(ResultCode.ADMIN_DISABLED.getCode(), ex.getCode());
        verify(guideMapper, never()).insert(any());
    }

    @Test
    void getAdminGuideList_includesAdminName() {
        Guide guide = new Guide();
        guide.setId(1L);
        guide.setTitle("西湖攻略");
        guide.setAdminId(9L);
        guide.setIsDeleted(0);
        guide.setIsPublished(1);
        guide.setViewCount(12);

        Admin admin = new Admin();
        admin.setId(9L);
        admin.setRealName("系统管理员");
        admin.setUsername("admin");

        Page<Guide> page = new Page<>(1, 10);
        page.setRecords(List.of(guide));
        page.setTotal(1L);

        when(guideMapper.selectPage(any(), any())).thenReturn(page);
        when(adminMapper.selectBatchIds(List.of(9L))).thenReturn(List.of(admin));

        PageResult<com.travel.dto.guide.response.AdminGuideListResponse> result =
            guideAdminService.getAdminGuideList(new com.travel.dto.guide.request.AdminGuideListRequest());

        assertEquals(1, result.getList().size());
        assertEquals(9L, result.getList().get(0).getAdminId());
        assertEquals("系统管理员", result.getList().get(0).getAdminName());
    }

    @Test
    void getAdminGuideDetail_includesAdminName() {
        Guide guide = new Guide();
        guide.setId(1L);
        guide.setTitle("西湖攻略");
        guide.setAdminId(9L);
        guide.setCategory("自然风光");
        guide.setContent("详情内容");
        guide.setIsDeleted(0);
        guide.setIsPublished(1);
        guide.setViewCount(12);

        Admin admin = new Admin();
        admin.setId(9L);
        admin.setRealName("系统管理员");
        admin.setUsername("admin");

        when(guideMapper.selectById(1L)).thenReturn(guide);
        when(adminMapper.selectById(9L)).thenReturn(admin);
        when(guideSpotRelationMapper.selectList(any())).thenReturn(List.of());

        AdminGuideRequest result = guideAdminService.getAdminGuideDetail(1L);

        assertEquals(9L, result.getAdminId());
        assertEquals("系统管理员", result.getAdminName());
        assertEquals("西湖攻略", result.getTitle());
    }

    @Test
    void getAdminGuideList_returnsPurgedAdminWhenAdminRecordRemoved() {
        Guide guide = new Guide();
        guide.setId(2L);
        guide.setTitle("灵隐寺攻略");
        guide.setAdminId(10L);
        guide.setIsDeleted(0);
        guide.setIsPublished(1);
        guide.setViewCount(8);

        Page<Guide> page = new Page<>(1, 10);
        page.setRecords(List.of(guide));
        page.setTotal(1L);

        when(guideMapper.selectPage(any(), any())).thenReturn(page);
        when(adminMapper.selectBatchIds(List.of(10L))).thenReturn(List.of());

        PageResult<com.travel.dto.guide.response.AdminGuideListResponse> result =
            guideAdminService.getAdminGuideList(new com.travel.dto.guide.request.AdminGuideListRequest());

        assertEquals("已清除管理员", result.getList().get(0).getAdminName());
    }

    @Test
    void getAdminGuideList_returnsDeactivatedAdminWhenAdminDisabled() {
        Guide guide = new Guide();
        guide.setId(4L);
        guide.setTitle("九溪攻略");
        guide.setAdminId(11L);
        guide.setIsDeleted(0);
        guide.setIsPublished(1);
        guide.setViewCount(9);

        Admin admin = new Admin();
        admin.setId(11L);
        admin.setRealName("巡检管理员");
        admin.setUsername("checker");
        admin.setIsEnabled(0);

        Page<Guide> page = new Page<>(1, 10);
        page.setRecords(List.of(guide));
        page.setTotal(1L);

        when(guideMapper.selectPage(any(), any())).thenReturn(page);
        when(adminMapper.selectBatchIds(List.of(11L))).thenReturn(List.of(admin));

        PageResult<com.travel.dto.guide.response.AdminGuideListResponse> result =
            guideAdminService.getAdminGuideList(new com.travel.dto.guide.request.AdminGuideListRequest());

        assertEquals("已停用管理员", result.getList().get(0).getAdminName());
    }

    @Test
    void getAdminGuideDetail_returnsUnknownAdminWhenGuideHasNoAdminId() {
        Guide guide = new Guide();
        guide.setId(3L);
        guide.setTitle("断桥攻略");
        guide.setAdminId(null);
        guide.setCategory("城市漫游");
        guide.setContent("详情内容");
        guide.setIsDeleted(0);
        guide.setIsPublished(1);
        guide.setViewCount(5);

        when(guideMapper.selectById(3L)).thenReturn(guide);
        when(guideSpotRelationMapper.selectList(any())).thenReturn(List.of());

        AdminGuideRequest result = guideAdminService.getAdminGuideDetail(3L);

        assertEquals("未知管理员", result.getAdminName());
    }

    @Test
    void getAdminGuideDetail_returnsDeactivatedAdminWhenAdminDisabled() {
        Guide guide = new Guide();
        guide.setId(5L);
        guide.setTitle("湘湖攻略");
        guide.setAdminId(12L);
        guide.setCategory("周末短途");
        guide.setContent("详情内容");
        guide.setIsDeleted(0);
        guide.setIsPublished(1);
        guide.setViewCount(6);

        Admin admin = new Admin();
        admin.setId(12L);
        admin.setRealName("内容管理员");
        admin.setUsername("content-admin");
        admin.setIsEnabled(0);

        when(guideMapper.selectById(5L)).thenReturn(guide);
        when(adminMapper.selectById(12L)).thenReturn(admin);
        when(guideSpotRelationMapper.selectList(any())).thenReturn(List.of());

        AdminGuideRequest result = guideAdminService.getAdminGuideDetail(5L);

        assertEquals("已停用管理员", result.getAdminName());
    }
}
