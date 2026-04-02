package com.travel.service.impl;

import com.travel.dto.banner.request.AdminBannerRequest;
import com.travel.entity.SpotBanner;
import com.travel.mapper.SpotBannerMapper;
import com.travel.mapper.SpotMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 轮播图排序服务测试
 * 重点覆盖插入顺延、编辑重排和删除补位三个核心场景。
 */
@ExtendWith(MockitoExtension.class)
class SpotBannerServiceImplTest {

    @Mock
    private SpotBannerMapper spotBannerMapper;

    @Mock
    private SpotMapper spotMapper;

    private SpotBannerServiceImpl spotBannerService;

    /**
     * 手动注入构造依赖，保持测试聚焦在排序逻辑本身。
     */
    @BeforeEach
    void setUp() {
        spotBannerService = new SpotBannerServiceImpl(spotBannerMapper, spotMapper);
    }

    @Test
    void createBanner_shouldShiftFollowingItems_whenRequestedSortAlreadyExists() {
        SpotBanner first = buildBanner(1L, 1);
        SpotBanner second = buildBanner(2L, 2);

        when(spotBannerMapper.selectList(any())).thenReturn(List.of(first, second));
        when(spotBannerMapper.updateById(any())).thenReturn(1);
        when(spotBannerMapper.insert(any())).thenReturn(1);

        AdminBannerRequest request = new AdminBannerRequest();
        request.setImageUrl("/banner/new.jpg");
        request.setSortOrder(2);
        request.setEnabled(1);

        spotBannerService.createBanner(request);

        verify(spotBannerMapper).updateById(second);
        assertEquals(3, second.getSortOrder());

        ArgumentCaptor<SpotBanner> insertCaptor = ArgumentCaptor.forClass(SpotBanner.class);
        verify(spotBannerMapper).insert(insertCaptor.capture());
        assertEquals(2, insertCaptor.getValue().getSortOrder());
    }

    @Test
    void updateBanner_shouldShiftAffectedRange_whenMoveUp() {
        SpotBanner current = buildBanner(3L, 3);
        SpotBanner first = buildBanner(1L, 1);
        SpotBanner second = buildBanner(2L, 2);
        SpotBanner fourth = buildBanner(4L, 4);

        when(spotBannerMapper.selectById(3L)).thenReturn(current);
        when(spotBannerMapper.selectList(any())).thenReturn(List.of(first, second, current, fourth));
        when(spotBannerMapper.updateById(any())).thenReturn(1);

        AdminBannerRequest request = new AdminBannerRequest();
        request.setImageUrl("/banner/edit.jpg");
        request.setSortOrder(2);
        request.setEnabled(1);

        spotBannerService.updateBanner(3L, request);

        verify(spotBannerMapper, times(2)).updateById(any());
        assertEquals(3, second.getSortOrder());
        assertEquals(2, current.getSortOrder());
    }

    @Test
    void deleteBanner_shouldCompactFollowingItems_whenDeleteSucceeds() {
        SpotBanner current = buildBanner(2L, 2);
        SpotBanner following = buildBanner(3L, 3);

        when(spotBannerMapper.selectById(2L)).thenReturn(current);
        when(spotBannerMapper.updateById(any())).thenReturn(1);
        when(spotBannerMapper.selectList(any())).thenReturn(List.of(current, following));

        spotBannerService.deleteBanner(2L);

        verify(spotBannerMapper).updateById(argThat(item -> item.getId().equals(2L) && item.getIsDeleted() == 1));
        verify(spotBannerMapper).updateById(argThat(item -> item.getId().equals(3L) && item.getSortOrder() == 2));
        assertEquals(2, following.getSortOrder());
    }

    /**
     * 构造有效轮播图夹具，便于测试重排逻辑。
     */
    private SpotBanner buildBanner(Long id, Integer sortOrder) {
        SpotBanner banner = new SpotBanner();
        banner.setId(id);
        banner.setImageUrl("/banner/" + id + ".jpg");
        banner.setSortOrder(sortOrder);
        banner.setIsEnabled(1);
        banner.setIsDeleted(0);
        return banner;
    }
}
