package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.travel.dto.recommendation.model.RecommendationConfigBundleDTO;
import com.travel.entity.Order;
import com.travel.entity.Review;
import com.travel.entity.Spot;
import com.travel.entity.SpotCategory;
import com.travel.entity.SpotImage;
import com.travel.entity.SpotRegion;
import com.travel.entity.UserSpotFavorite;
import com.travel.entity.UserSpotView;
import com.travel.mapper.OrderMapper;
import com.travel.mapper.ReviewMapper;
import com.travel.mapper.SpotCategoryMapper;
import com.travel.mapper.SpotImageMapper;
import com.travel.mapper.SpotMapper;
import com.travel.mapper.SpotRegionMapper;
import com.travel.mapper.UserSpotFavoriteMapper;
import com.travel.mapper.UserSpotViewMapper;
import com.travel.service.RecommendationService;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 景点服务测试
 * 重点覆盖热度分同步逻辑。
 */
@ExtendWith(MockitoExtension.class)
class SpotServiceImplTest {

    /**
     * 初始化 MyBatis-Plus Lambda 缓存，确保 LambdaQueryWrapper 可正常解析字段。
     */
    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "test");
        assistant.setCurrentNamespace("test");
        TableInfoHelper.initTableInfo(assistant, Spot.class);
        TableInfoHelper.initTableInfo(assistant, SpotImage.class);
        TableInfoHelper.initTableInfo(assistant, SpotRegion.class);
        TableInfoHelper.initTableInfo(assistant, SpotCategory.class);
        TableInfoHelper.initTableInfo(assistant, UserSpotFavorite.class);
        TableInfoHelper.initTableInfo(assistant, Review.class);
        TableInfoHelper.initTableInfo(assistant, UserSpotView.class);
        TableInfoHelper.initTableInfo(assistant, Order.class);
    }

    @Mock
    private SpotMapper spotMapper;

    @Mock
    private SpotImageMapper spotImageMapper;

    @Mock
    private SpotRegionMapper spotRegionMapper;

    @Mock
    private SpotCategoryMapper spotCategoryMapper;

    @Mock
    private UserSpotFavoriteMapper userSpotFavoriteMapper;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private UserSpotViewMapper userSpotViewMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private RecommendationService recommendationService;

    private SpotServiceImpl spotService;

    @BeforeEach
    void setUp() {
        spotService = new SpotServiceImpl(
            spotMapper,
            spotImageMapper,
            spotRegionMapper,
            spotCategoryMapper,
            userSpotFavoriteMapper,
            reviewMapper,
            userSpotViewMapper,
            orderMapper,
            recommendationService
        );
    }

    @Test
    void refreshSpotHeat_setsHeatScoreToBasePlusBehaviorScore() {
        Spot spot = new Spot();
        spot.setId(1L);
        spot.setHeatLevel(2);
        spot.setIsDeleted(0);

        when(spotMapper.selectById(1L)).thenReturn(spot);
        when(userSpotViewMapper.selectCount(any())).thenReturn(10L);
        when(userSpotFavoriteMapper.selectCount(any())).thenReturn(2L);
        when(reviewMapper.selectCount(any())).thenReturn(1L);
        when(orderMapper.selectCount(any())).thenReturn(3L, 1L);
        when(recommendationService.getConfig()).thenReturn(new RecommendationConfigBundleDTO());

        spotService.refreshSpotHeat(1L);

        ArgumentCaptor<UpdateWrapper<Spot>> wrapperCaptor = ArgumentCaptor.forClass(UpdateWrapper.class);
        verify(spotMapper).update(isNull(), wrapperCaptor.capture());
        Map<String, Object> params = wrapperCaptor.getValue().getParamNameValuePairs();
        assertEquals(541, params.values().iterator().next());
    }

    @Test
    void refreshAllSpotHeat_updatesEachActiveSpot() {
        Spot first = new Spot();
        first.setId(1L);
        first.setHeatLevel(0);

        Spot second = new Spot();
        second.setId(2L);
        second.setHeatLevel(1);

        when(spotMapper.selectList(any())).thenReturn(List.of(first, second));
        when(recommendationService.getConfig()).thenReturn(new RecommendationConfigBundleDTO());

        spotService.refreshAllSpotHeat();

        ArgumentCaptor<UpdateWrapper<Spot>> wrapperCaptor = ArgumentCaptor.forClass(UpdateWrapper.class);
        verify(spotMapper, times(2)).update(isNull(), wrapperCaptor.capture());
        List<UpdateWrapper<Spot>> wrappers = wrapperCaptor.getAllValues();
        assertEquals(0, wrappers.get(0).getParamNameValuePairs().values().iterator().next());
        assertEquals(200, wrappers.get(1).getParamNameValuePairs().values().iterator().next());
    }
}
