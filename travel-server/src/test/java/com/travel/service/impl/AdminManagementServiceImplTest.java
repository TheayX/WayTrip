package com.travel.service.impl;

import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.entity.Admin;
import com.travel.entity.Guide;
import com.travel.mapper.AdminMapper;
import com.travel.mapper.GuideMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理员服务测试
 * 重点覆盖管理员删除前的应用层关联保护。
 */
@ExtendWith(MockitoExtension.class)
class AdminManagementServiceImplTest {

    @Mock
    private AdminMapper adminMapper;

    @Mock
    private GuideMapper guideMapper;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminManagementServiceImpl adminManagementService;

    @Test
    void deleteAdmin_rejectsWhenActiveGuideStillReferencesAdmin() {
        Admin admin = new Admin();
        admin.setId(2L);
        admin.setUsername("guide-admin");
        admin.setIsDeleted(0);
        when(adminMapper.selectById(2L)).thenReturn(admin);
        when(guideMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> adminManagementService.deleteAdmin(2L, 1L));

        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
        assertEquals("该管理员下仍有关联攻略，请先转移或删除攻略", ex.getMessage());
        verify(adminMapper, never()).updateById(any(Admin.class));
    }
}
