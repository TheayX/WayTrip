package com.travel.service.impl;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.travel.dto.recommendation.model.RecommendationAlgorithmConfigDTO;
import com.travel.dto.recommendation.response.RecommendationResponse;
import com.travel.entity.Order;
import com.travel.entity.Review;
import com.travel.entity.Spot;
import com.travel.entity.SpotCategory;
import com.travel.entity.SpotRegion;
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
import com.travel.service.cache.RecommendationCacheService;
import com.travel.service.support.recommendation.RecommendationCandidateSupport;
import com.travel.service.support.recommendation.RecommendationConfigSupport;
import com.travel.service.support.recommendation.RecommendationColdStartSupport;
import com.travel.service.support.recommendation.RecommendationDebugSupport;
import com.travel.service.support.recommendation.RecommendationInteractionSupport;
import com.travel.service.support.recommendation.RecommendationMetadataSupport;
import com.travel.service.support.recommendation.RecommendationResponseSupport;
import com.travel.service.support.recommendation.RecommendationSimilaritySupport;
import com.travel.service.support.recommendation.RecommendationViewSourceClassifier;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 推荐服务测试
 * 重点覆盖用户行为权重计算和冷启动兜底策略。
 */
@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {

    /**
     * 初始化 MyBatis-Plus Lambda 缓存，保证推荐逻辑里的 Lambda 查询可用。
     */
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
    private RecommendationCacheService recommendationCacheService;

    private RecommendationServiceImpl recommendationService;

    /**
     * 构建推荐服务测试对象及其缓存依赖。
     */
    @BeforeEach
    void setUp() {
        RecommendationMetadataSupport recommendationMetadataSupport = new RecommendationMetadataSupport(
            spotMapper,
            categoryMapper,
            spotRegionMapper
        );
        RecommendationConfigSupport recommendationConfigSupport = new RecommendationConfigSupport(recommendationCacheService);
        RecommendationSimilaritySupport recommendationSimilaritySupport = new RecommendationSimilaritySupport(
            spotMapper,
            recommendationCacheService,
            recommendationMetadataSupport
        );
        RecommendationViewSourceClassifier recommendationViewSourceClassifier = new RecommendationViewSourceClassifier();
        RecommendationInteractionSupport recommendationInteractionSupport = new RecommendationInteractionSupport(
            userSpotViewMapper,
            userSpotFavoriteMapper,
            reviewMapper,
            orderMapper,
            recommendationViewSourceClassifier
        );
        RecommendationColdStartSupport recommendationColdStartSupport = new RecommendationColdStartSupport(
            spotMapper,
            userPreferenceMapper
        );
        RecommendationResponseSupport recommendationResponseSupport = new RecommendationResponseSupport(
            spotMapper,
            recommendationMetadataSupport
        );
        RecommendationDebugSupport recommendationDebugSupport = new RecommendationDebugSupport(
            userSpotViewMapper,
            userSpotFavoriteMapper,
            reviewMapper,
            orderMapper,
            recommendationMetadataSupport,
            recommendationInteractionSupport
        );
        RecommendationCandidateSupport recommendationCandidateSupport = new RecommendationCandidateSupport(
            spotMapper,
            reviewMapper,
            userSpotFavoriteMapper,
            orderMapper,
            recommendationDebugSupport
        );

        recommendationService = new RecommendationServiceImpl(
            spotMapper,
            reviewMapper,
            userSpotFavoriteMapper,
            orderMapper,
            userSpotViewMapper,
            categoryMapper,
            spotRegionMapper,
            userPreferenceMapper,
            recommendationCacheService,
            recommendationMetadataSupport,
            recommendationConfigSupport,
            recommendationSimilaritySupport,
            recommendationInteractionSupport,
            recommendationCandidateSupport,
            recommendationColdStartSupport,
            recommendationResponseSupport,
            recommendationDebugSupport
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

        RecommendationAlgorithmConfigDTO config = new RecommendationAlgorithmConfigDTO();

        Map<Long, Double> weights = (Map<Long, Double>) ReflectionTestUtils.invokeMethod(
            recommendationService,
            "buildUserInteractionWeights",
            1L,
            config
        );

        assertEquals(1, weights.size());
        assertEquals(6.6, weights.get(100L), 0.0001);
    }

    @Test
    void handleColdStart_fallsBackToHotWhenPreferredCategoriesHaveNoSpots() {
        UserPreference preference = new UserPreference();
        preference.setUserId(1L);
        preference.setTag("10");
        preference.setIsDeleted(0);

        Spot hotSpot1 = buildSpot(201L, "热门景点1", 20L);
        Spot hotSpot2 = buildSpot(202L, "热门景点2", 21L);
        Spot hotSpot3 = buildSpot(203L, "热门景点3", 22L);
        Spot hotSpot4 = buildSpot(204L, "热门景点4", 23L);

        when(userPreferenceMapper.selectList(any())).thenReturn(List.of(preference));
        when(spotMapper.selectList(any())).thenReturn(
            List.of(),
            List.of(hotSpot1, hotSpot2, hotSpot3, hotSpot4)
        );
        when(spotMapper.selectBatchIds(any())).thenReturn(List.of(hotSpot1, hotSpot2, hotSpot3, hotSpot4));
        mockCategoryAndRegionMaps();

        RecommendationResponse response = (RecommendationResponse) ReflectionTestUtils.invokeMethod(
            recommendationService,
            "handleColdStart",
            1L,
            4,
            false,
            false
        );

        assertEquals("preference", response.getType());
        assertFalse(response.getNeedPreference());
        assertEquals(4, response.getList().size());
        assertEquals(List.of(201L, 202L, 203L, 204L), response.getList().stream().map(RecommendationResponse.SpotItem::getId).toList());
    }

    @Test
    void handleColdStart_supplementsHotSpotsWhenPreferredSpotsAreInsufficient() {
        UserPreference preference = new UserPreference();
        preference.setUserId(1L);
        preference.setTag("10");
        preference.setIsDeleted(0);

        Spot preferredSpot1 = buildSpot(101L, "偏好景点1", 10L);
        Spot preferredSpot2 = buildSpot(102L, "偏好景点2", 10L);
        Spot hotSpot1 = buildSpot(201L, "热门景点1", 20L);
        Spot hotSpot2 = buildSpot(202L, "热门景点2", 21L);

        when(userPreferenceMapper.selectList(any())).thenReturn(List.of(preference));
        when(spotMapper.selectList(any())).thenReturn(
            List.of(preferredSpot1, preferredSpot2),
            List.of(hotSpot1, hotSpot2)
        );
        when(spotMapper.selectBatchIds(any())).thenReturn(List.of(preferredSpot1, preferredSpot2, hotSpot1, hotSpot2));
        mockCategoryAndRegionMaps();

        RecommendationResponse response = (RecommendationResponse) ReflectionTestUtils.invokeMethod(
            recommendationService,
            "handleColdStart",
            1L,
            4,
            false,
            false
        );

        assertEquals("preference", response.getType());
        assertFalse(response.getNeedPreference());
        assertEquals(4, response.getList().size());
        assertEquals(List.of(101L, 102L, 201L, 202L), response.getList().stream().map(RecommendationResponse.SpotItem::getId).toList());
    }

    /**
     * 构造推荐结果中的景点夹具。
     */
    private Spot buildSpot(Long id, String name, Long categoryId) {
        Spot spot = new Spot();
        spot.setId(id);
        spot.setName(name);
        spot.setCategoryId(categoryId);
        spot.setRegionId(1L);
        spot.setIsPublished(1);
        spot.setIsDeleted(0);
        return spot;
    }

    /**
     * 模拟分类与地区字典，供推荐结果组装时使用。
     */
    private void mockCategoryAndRegionMaps() {
        SpotCategory category = new SpotCategory();
        category.setId(10L);
        category.setName("分类");
        category.setIsDeleted(0);

        SpotRegion region = new SpotRegion();
        region.setId(1L);
        region.setName("区域");
        region.setIsDeleted(0);

        when(categoryMapper.selectList(any())).thenReturn(List.of(category));
        when(spotRegionMapper.selectList(any())).thenReturn(List.of(region));
    }
}
