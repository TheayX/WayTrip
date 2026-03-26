package com.travel.service.impl;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.travel.dto.recommendation.LegacyRecommendationConfigDTO;
import com.travel.entity.Order;
import com.travel.entity.Review;
import com.travel.entity.Spot;
import com.travel.entity.UserPreference;
import com.travel.entity.UserSpotFavorite;
import com.travel.entity.UserSpotView;
import com.travel.enums.OrderStatus;
import com.travel.mapper.OrderMapper;
import com.travel.mapper.ReviewMapper;
import com.travel.mapper.SpotCategoryMapper;
import com.travel.mapper.SpotMapper;
import com.travel.mapper.SpotRegionMapper;
import com.travel.mapper.UserPreferenceMapper;
import com.travel.mapper.UserSpotFavoriteMapper;
import com.travel.mapper.UserSpotViewMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {

    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "test");
        assistant.setCurrentNamespace("test");
        TableInfoHelper.initTableInfo(assistant, UserSpotView.class);
        TableInfoHelper.initTableInfo(assistant, UserSpotFavorite.class);
        TableInfoHelper.initTableInfo(assistant, Review.class);
        TableInfoHelper.initTableInfo(assistant, Order.class);
        TableInfoHelper.initTableInfo(assistant, Spot.class);
        TableInfoHelper.initTableInfo(assistant, UserPreference.class);
    }

    @Mock
    private SpotMapper spotMapper;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private UserSpotFavoriteMapper userSpotFavoriteMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private UserSpotViewMapper userSpotViewMapper;

    @Mock
    private SpotCategoryMapper categoryMapper;

    @Mock
    private SpotRegionMapper spotRegionMapper;

    @Mock
    private UserPreferenceMapper userPreferenceMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RecommendationServiceImpl recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationServiceImpl(
            spotMapper,
            reviewMapper,
            userSpotFavoriteMapper,
            orderMapper,
            userSpotViewMapper,
            categoryMapper,
            spotRegionMapper,
            userPreferenceMapper,
            redisTemplate
        );
    }

    @Test
    void buildUserInteractionWeights_sumsMultipleSignalsForSameSpot() {
        UserSpotView view = new UserSpotView();
        view.setUserId(1L);
        view.setSpotId(100L);
        view.setViewSource("detail");
        view.setViewDuration(90);

        UserSpotFavorite favorite = new UserSpotFavorite();
        favorite.setUserId(1L);
        favorite.setSpotId(100L);
        favorite.setIsDeleted(0);

        Review review = new Review();
        review.setUserId(1L);
        review.setSpotId(100L);
        review.setScore(5);
        review.setIsDeleted(0);

        Order order = new Order();
        order.setUserId(1L);
        order.setSpotId(100L);
        order.setStatus(OrderStatus.PAID.getCode());
        order.setIsDeleted(0);

        when(userSpotViewMapper.selectList(any())).thenReturn(List.of(view));
        when(userSpotFavoriteMapper.selectList(any())).thenReturn(List.of(favorite));
        when(reviewMapper.selectList(any())).thenReturn(List.of(review));
        when(orderMapper.selectList(any())).thenReturn(List.of(order));

        LegacyRecommendationConfigDTO config = LegacyRecommendationConfigDTO.defaultConfig();

        Map<Long, Double> weights = (Map<Long, Double>) ReflectionTestUtils.invokeMethod(
            recommendationService,
            "buildUserInteractionWeights",
            1L,
            config
        );

        assertEquals(1, weights.size());
        assertEquals(6.6, weights.get(100L), 0.0001);
    }
}
