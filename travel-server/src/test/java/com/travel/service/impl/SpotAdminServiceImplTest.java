package com.travel.service.impl;

import com.travel.common.exception.BusinessException;
import com.travel.common.result.PageResult;
import com.travel.common.result.ResultCode;
import com.travel.dto.spot.request.AdminSpotListRequest;
import com.travel.dto.spot.response.AdminSpotListResponse;
import com.travel.entity.Spot;
import com.travel.mapper.GuideSpotRelationMapper;
import com.travel.mapper.OrderMapper;
import com.travel.mapper.SpotBannerMapper;
import com.travel.mapper.SpotImageMapper;
import com.travel.mapper.SpotMapper;
import com.travel.mapper.UserSpotFavoriteMapper;
import com.travel.mapper.UserSpotViewMapper;
import com.travel.service.RecommendationService;
import com.travel.service.support.spot.SpotResponseAssembler;
import com.travel.service.support.spot.SpotTreeSupport;
import com.travel.service.support.spot.SpotWriteSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 景点管理服务测试
 * 重点覆盖景点列表筛选与删除链路。
 */
@ExtendWith(MockitoExtension.class)
class SpotAdminServiceImplTest {

    @Mock
    private SpotMapper spotMapper;

    @Mock
    private SpotImageMapper spotImageMapper;

    @Mock
    private SpotBannerMapper spotBannerMapper;

    @Mock
    private GuideSpotRelationMapper guideSpotRelationMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private UserSpotFavoriteMapper userSpotFavoriteMapper;

    @Mock
    private UserSpotViewMapper userSpotViewMapper;

    @Mock
    private SpotResponseAssembler spotResponseAssembler;

    @Mock
    private SpotTreeSupport spotTreeSupport;

    @Mock
    private SpotWriteSupport spotWriteSupport;

    @Mock
    private RecommendationService recommendationService;

    @InjectMocks
    private SpotAdminServiceImpl spotAdminService;

    @Test
    void getAdminSpotList_acceptsHeatLevelFilter() {
        Spot spot = new Spot();
        spot.setId(5L);
        spot.setName("西湖");
        spot.setHeatLevel(2);
        spot.setIsDeleted(0);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Spot> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        page.setRecords(List.of(spot));
        page.setTotal(1L);

        AdminSpotListRequest request = new AdminSpotListRequest();
        request.setPage(1);
        request.setPageSize(10);
        request.setHeatLevel(2);

        when(spotMapper.selectPage(any(), any())).thenReturn(page);
        when(spotResponseAssembler.toAdminSpotListResponse(any())).thenReturn(mock(AdminSpotListResponse.class));

        PageResult<AdminSpotListResponse> response = spotAdminService.getAdminSpotList(request);

        assertEquals(1L, response.getTotal());
        assertEquals(1, response.getList().size());
    }

    @Test
    void deleteSpot_marksSpotDeleted() {
        Spot spot = new Spot();
        spot.setId(5L);
        spot.setName("西湖");
        spot.setIsDeleted(0);
        when(spotMapper.selectById(5L)).thenReturn(spot);

        when(spotBannerMapper.selectList(any())).thenReturn(List.of());

        spotAdminService.deleteSpot(5L);

        assertEquals(1, spot.getIsDeleted());
    }

    @Test
    void deleteSpot_rejectsMissingSpot() {
        when(spotMapper.selectById(5L)).thenReturn(null);

        BusinessException ex = org.junit.jupiter.api.Assertions.assertThrows(BusinessException.class, () -> spotAdminService.deleteSpot(5L));

        assertEquals(ResultCode.SPOT_NOT_FOUND.getCode(), ex.getCode());
        assertTrue(ex.getMessage() == null || !ex.getMessage().isBlank());
    }
}
