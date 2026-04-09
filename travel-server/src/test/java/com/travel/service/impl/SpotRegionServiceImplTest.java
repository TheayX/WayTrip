package com.travel.service.impl;

import com.travel.dto.region.request.AdminRegionRequest;
import com.travel.entity.Spot;
import com.travel.entity.SpotRegion;
import com.travel.mapper.SpotMapper;
import com.travel.mapper.SpotRegionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 地区排序服务测试
 * 重点覆盖跨父级移动时旧层级补位和新层级顺延。
 */
@ExtendWith(MockitoExtension.class)
class SpotRegionServiceImplTest {

    @Mock
    private SpotRegionMapper spotRegionMapper;

    @Mock
    private SpotMapper spotMapper;

    private SpotRegionServiceImpl spotRegionService;

    /**
     * 将 MyBatis Mapper 注入到 ServiceImpl 父类字段，便于直接做单元测试。
     */
    @BeforeEach
    void setUp() {
        spotRegionService = new SpotRegionServiceImpl(spotMapper);
        ReflectionTestUtils.setField(spotRegionService, "baseMapper", spotRegionMapper);
    }

    @Test
    void updateRegion_shouldCompactOldParentAndShiftNewParent_whenParentChanges() {
        SpotRegion current = buildRegion(3L, 10L, 2);
        SpotRegion oldFollowing = buildRegion(4L, 10L, 3);
        SpotRegion targetFirst = buildRegion(5L, 20L, 1);
        SpotRegion targetSecond = buildRegion(6L, 20L, 2);

        when(spotRegionMapper.selectById(3L)).thenReturn(current);
        when(spotRegionMapper.selectById(20L)).thenReturn(buildRegion(20L, null, 1));
        when(spotRegionMapper.selectList(any())).thenReturn(
            List.of(current, oldFollowing),
            List.of(targetFirst, targetSecond)
        );
        when(spotRegionMapper.updateById(any())).thenReturn(1);

        AdminRegionRequest request = new AdminRegionRequest();
        request.setParentId(20L);
        request.setName("迁移地区");
        request.setSortOrder(2);

        spotRegionService.updateRegion(3L, request);

        assertEquals(2, oldFollowing.getSortOrder());
        assertEquals(3, targetSecond.getSortOrder());

        ArgumentCaptor<SpotRegion> updateCaptor = ArgumentCaptor.forClass(SpotRegion.class);
        verify(spotRegionMapper, org.mockito.Mockito.times(3)).updateById(updateCaptor.capture());
        SpotRegion updatedCurrent = updateCaptor.getAllValues().get(2);
        assertEquals(20L, updatedCurrent.getParentId());
        assertEquals(2, updatedCurrent.getSortOrder());
    }

    @Test
    void deleteRegion_shouldCompactFollowingSiblings_whenDeleteSucceeds() {
        SpotRegion current = buildRegion(2L, 10L, 2);
        SpotRegion following = buildRegion(3L, 10L, 3);

        when(spotRegionMapper.selectById(2L)).thenReturn(current);
        when(spotRegionMapper.selectCount(any())).thenReturn(0L);
        when(spotMapper.selectCount(any())).thenReturn(0L);
        when(spotRegionMapper.updateById(any())).thenReturn(1);
        when(spotRegionMapper.selectList(any())).thenReturn(List.of(current, following));

        spotRegionService.deleteRegion(2L);

        verify(spotRegionMapper).updateById(argThat(item -> item.getId().equals(2L) && item.getIsDeleted() == 1));
        verify(spotRegionMapper).updateById(argThat(item -> item.getId().equals(3L) && item.getSortOrder() == 2));
        assertEquals(2, following.getSortOrder());
    }

    @Test
    void deleteRegion_rejectsWhenActiveSpotStillReferencesRegion() {
        SpotRegion current = buildRegion(2L, 10L, 2);

        when(spotRegionMapper.selectById(2L)).thenReturn(current);
        when(spotRegionMapper.selectCount(any())).thenReturn(0L);
        when(spotMapper.selectCount(any())).thenReturn(1L);

        RuntimeException ex = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
            () -> spotRegionService.deleteRegion(2L));

        assertEquals("该地区下仍有关联景点，请先调整景点地区", ex.getMessage());
    }

    /**
     * 按给定父节点和排序构造有效地区夹具。
     */
    private SpotRegion buildRegion(Long id, Long parentId, Integer sortOrder) {
        SpotRegion region = new SpotRegion();
        region.setId(id);
        region.setParentId(parentId);
        region.setName("地区" + id);
        region.setSortOrder(sortOrder);
        region.setIsDeleted(0);
        return region;
    }
}
