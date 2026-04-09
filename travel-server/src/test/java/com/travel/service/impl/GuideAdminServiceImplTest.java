package com.travel.service.impl;

import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.dto.guide.request.AdminGuideRequest;
import com.travel.entity.Admin;
import com.travel.mapper.AdminMapper;
import com.travel.mapper.GuideMapper;
import com.travel.mapper.GuideSpotRelationMapper;
import com.travel.mapper.SpotMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
