package com.travel.service.impl;

import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.entity.Admin;
import com.travel.mapper.AdminMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理员服务测试
 * 重点覆盖管理员删除与自删除保护。
 */
@ExtendWith(MockitoExtension.class)
class AdminManagementServiceImplTest {

    @Mock
    private AdminMapper adminMapper;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminManagementServiceImpl adminManagementService;

    @Test
    void deleteAdmin_allowsHistoricalGuideReferenceToRemain() {
        Admin admin = new Admin();
        admin.setId(2L);
        admin.setUsername("guide-admin");
        admin.setIsDeleted(0);
        when(adminMapper.selectById(2L)).thenReturn(admin);
        
        adminManagementService.deleteAdmin(2L, 1L);

        verify(adminMapper).updateById(any(Admin.class));
    }

    @Test
    void deleteAdmin_rejectsSelfDelete() {
        BusinessException ex = assertThrows(BusinessException.class,
            () -> adminManagementService.deleteAdmin(2L, 2L));

        assertEquals(ResultCode.ADMIN_SELF_OPERATION_FORBIDDEN.getCode(), ex.getCode());
    }
}
