package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.dto.spot.request.SpotListRequest;
import com.travel.entity.Review;
import com.travel.entity.Spot;
import com.travel.entity.SpotImage;
import com.travel.entity.UserSpotFavorite;
import com.travel.entity.UserSpotView;
import com.travel.mapper.ReviewMapper;
import com.travel.mapper.SpotImageMapper;
import com.travel.mapper.SpotMapper;
import com.travel.mapper.UserSpotFavoriteMapper;
import com.travel.mapper.UserSpotViewMapper;
import com.travel.service.support.spot.SpotResponseAssembler;
import com.travel.service.support.spot.SpotTreeSupport;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 景点查询服务测试。
 * 重点覆盖用户端发布状态过滤的关键链路。
 */
@ExtendWith(MockitoExtension.class)
class SpotQueryServiceImplTest {

    /**
     * 初始化 MyBatis-Plus Lambda 缓存，保证 Lambda 条件构建不会在单测中报错。
     */
    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "test");
        assistant.setCurrentNamespace("test");
        TableInfoHelper.initTableInfo(assistant, Spot.class);
        TableInfoHelper.initTableInfo(assistant, SpotImage.class);
        TableInfoHelper.initTableInfo(assistant, UserSpotFavorite.class);
        TableInfoHelper.initTableInfo(assistant, UserSpotView.class);
        TableInfoHelper.initTableInfo(assistant, Review.class);
    }

    @Mock
    private SpotMapper spotMapper;

    @Mock
    private SpotImageMapper spotImageMapper;

    @Mock
    private UserSpotFavoriteMapper userSpotFavoriteMapper;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private UserSpotViewMapper userSpotViewMapper;

    @Mock
    private SpotResponseAssembler spotResponseAssembler;

    @Mock
    private SpotTreeSupport spotTreeSupport;

    private SpotQueryServiceImpl spotQueryService;

    @BeforeEach
    void setUp() {
        spotQueryService = new SpotQueryServiceImpl(
            spotMapper,
            spotImageMapper,
            userSpotFavoriteMapper,
            reviewMapper,
            userSpotViewMapper,
            spotResponseAssembler,
            spotTreeSupport
        );
    }

    @Test
    void getSpotList_returnsPagedRecords() {
        Spot spot = buildSpot(10L, 1);
        Page<Spot> page = new Page<>(1, 10);
        page.setRecords(List.of(spot));
        page.setTotal(1L);

        SpotListRequest request = new SpotListRequest();
        request.setRegionId(7L);
        request.setCategoryId(9L);
        when(spotTreeSupport.findRegionAndChildrenIds(7L)).thenReturn(Set.of(7L));
        when(spotTreeSupport.findCategoryAndChildrenIds(9L)).thenReturn(Set.of(9L));
        when(spotMapper.selectPage(any(), any())).thenReturn(page);
        when(spotResponseAssembler.toSpotListResponse(spot)).thenReturn(
            com.travel.dto.spot.response.SpotListResponse.builder().id(10L).name("已上架景点").build()
        );

        var response = spotQueryService.getSpotList(request);

        verify(spotMapper).selectPage(any(), any(LambdaQueryWrapper.class));
        assertEquals(1, response.getList().size());
        assertEquals(10L, response.getList().get(0).getId());
    }

    @Test
    void getViewHistory_filtersOfflineSpots() {
        UserSpotView publishedView = new UserSpotView();
        publishedView.setUserId(1L);
        publishedView.setSpotId(31L);
        publishedView.setCreatedAt(LocalDateTime.now().minusMinutes(3));

        UserSpotView offlineView = new UserSpotView();
        offlineView.setUserId(1L);
        offlineView.setSpotId(32L);
        offlineView.setCreatedAt(LocalDateTime.now().minusMinutes(1));

        Spot publishedSpot = buildSpot(31L, 1);
        Spot offlineSpot = buildSpot(32L, 0);

        when(userSpotViewMapper.selectList(any())).thenReturn(List.of(offlineView, publishedView));
        when(spotMapper.selectBatchIds(any())).thenReturn(List.of(publishedSpot, offlineSpot));
        when(spotResponseAssembler.getRegionName(any())).thenReturn("区域");
        when(spotResponseAssembler.getCategoryName(any())).thenReturn("分类");

        var response = spotQueryService.getViewHistory(1L, 1, 10);

        assertEquals(1L, response.getTotal());
        assertEquals(1, response.getList().size());
        assertEquals(31L, response.getList().get(0).getId());
    }

    @Test
    void getSpotDetail_rejectsOfflineSpot() {
        Spot offlineSpot = buildSpot(20L, 0);
        when(spotMapper.selectById(20L)).thenReturn(offlineSpot);

        BusinessException ex = assertThrows(BusinessException.class, () -> spotQueryService.getSpotDetail(20L, 1L));

        assertEquals(ResultCode.SPOT_OFFLINE.getCode(), ex.getCode());
        assertEquals(ResultCode.SPOT_OFFLINE.getMessage(), ex.getMessage());
        // 下架校验命中后应立即返回，避免多余数据库读取。
        verifyNoInteractions(spotImageMapper, userSpotFavoriteMapper, reviewMapper, userSpotViewMapper);
    }

    @Test
    void searchSpots_usesPublishedSearchMapperMethod() {
        Spot spot = buildSpot(66L, 1);
        Page<Spot> page = new Page<>(1, 10);
        page.setRecords(List.of(spot));
        page.setTotal(1L);

        when(spotMapper.selectPublishedSearchPage(any(), any())).thenReturn(page);
        when(spotResponseAssembler.toSpotListResponse(spot)).thenReturn(
            com.travel.dto.spot.response.SpotListResponse.builder().id(66L).name("已发布景点").build()
        );

        var response = spotQueryService.searchSpots("西湖", 1, 10);

        assertEquals(1L, response.getTotal());
        assertEquals(66L, response.getList().get(0).getId());
        verify(spotMapper).selectPublishedSearchPage(any(Page.class), any(String.class));
        verify(spotMapper, never()).selectPage(any(), any(LambdaQueryWrapper.class));
    }

    private Spot buildSpot(Long id, Integer published) {
        Spot spot = new Spot();
        spot.setId(id);
        spot.setName("测试景点");
        spot.setPrice(BigDecimal.valueOf(88));
        spot.setRegionId(1L);
        spot.setCategoryId(1L);
        spot.setIsPublished(published);
        spot.setIsDeleted(0);
        return spot;
    }
}

