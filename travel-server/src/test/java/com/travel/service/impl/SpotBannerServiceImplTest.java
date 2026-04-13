package com.travel.service.impl;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.travel.dto.banner.request.AdminBannerRequest;
import com.travel.dto.banner.response.BannerResponse;
import com.travel.entity.Spot;
import com.travel.entity.SpotBanner;
import com.travel.mapper.SpotBannerMapper;
import com.travel.mapper.SpotMapper;
import com.travel.service.cache.RecommendationCacheService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

/**
 * 轮播图排序服务测试
 * 重点覆盖插入顺延、编辑重排和删除补位三个核心场景。
 */
@ExtendWith(MockitoExtension.class)
class SpotBannerServiceImplTest {

    /**
     * 初始化 MyBatis-Plus Lambda 缓存，避免 LambdaUpdateWrapper 在单测中缺少实体元信息。
     */
    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "test");
        assistant.setCurrentNamespace("test");
        TableInfoHelper.initTableInfo(assistant, SpotBanner.class);
    }

    @Mock
    private SpotBannerMapper spotBannerMapper;

    @Mock
    private SpotMapper spotMapper;

    @Mock
    private RecommendationCacheService recommendationCacheService;

    private SpotBannerServiceImpl spotBannerService;

    /**
     * 手动注入构造依赖，保持测试聚焦在排序逻辑本身。
     */
    @BeforeEach
    void setUp() {
        spotBannerService = new SpotBannerServiceImpl(spotBannerMapper, spotMapper, recommendationCacheService);
    }

    @Test
    void getBanners_returnsCachedData_withoutDatabaseQuery() {
        BannerResponse cached = new BannerResponse();
        cached.setList(List.of(new BannerResponse.BannerItem(1L, "/banner/1.jpg", 100L, "景点", 1)));
        when(recommendationCacheService.getHomeBanners()).thenReturn(cached);

        BannerResponse response = spotBannerService.getBanners();

        assertEquals(1, response.getList().size());
        verifyNoInteractions(spotBannerMapper);
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
        verify(recommendationCacheService).deleteHomeBanners();

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

        verify(spotBannerMapper, times(1)).updateById(any());
        assertEquals(3, second.getSortOrder());
        verify(recommendationCacheService).deleteHomeBanners();
    }

    @Test
    void createBanner_rejectsDeletedSpotReference() {
        Spot deletedSpot = new Spot();
        deletedSpot.setId(100L);
        deletedSpot.setIsDeleted(1);
        when(spotMapper.selectById(100L)).thenReturn(deletedSpot);

        AdminBannerRequest request = new AdminBannerRequest();
        request.setImageUrl("/banner/new.jpg");
        request.setSpotId(100L);

        RuntimeException ex = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
            () -> spotBannerService.createBanner(request));

        assertEquals("关联景点不存在或已删除", ex.getMessage());
        verify(spotBannerMapper, never()).insert(any());
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
        verify(recommendationCacheService).deleteHomeBanners();
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
