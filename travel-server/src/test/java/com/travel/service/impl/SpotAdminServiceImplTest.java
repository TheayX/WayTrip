package com.travel.service.impl;

import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.entity.Spot;
import com.travel.mapper.GuideSpotRelationMapper;
import com.travel.mapper.OrderMapper;
import com.travel.mapper.ReviewMapper;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 景点管理服务测试
 * 重点覆盖景点删除前的应用层引用保护。
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
    private ReviewMapper reviewMapper;

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
    void deleteSpot_rejectsWhenBannerStillReferencesSpot() {
        Spot spot = new Spot();
        spot.setId(5L);
        spot.setName("西湖");
        spot.setIsDeleted(0);
        when(spotMapper.selectById(5L)).thenReturn(spot);
        when(spotBannerMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> spotAdminService.deleteSpot(5L));

        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
        assertEquals("该景点仍被轮播图引用，请先解除轮播关联", ex.getMessage());
        verify(spotMapper, never()).updateById(any(Spot.class));
    }
}
