package com.travel.service.support.recommendation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.dto.recommendation.model.RecommendationAlgorithmConfigDTO;
import com.travel.entity.Order;
import com.travel.entity.Review;
import com.travel.entity.UserSpotFavorite;
import com.travel.entity.UserSpotView;
import com.travel.enums.OrderStatus;
import com.travel.mapper.OrderMapper;
import com.travel.mapper.ReviewMapper;
import com.travel.mapper.UserSpotFavoriteMapper;
import com.travel.mapper.UserSpotViewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 推荐交互支撑，集中处理用户行为权重构建和浏览行为权重计算。
 */
@Component
@RequiredArgsConstructor
public class RecommendationInteractionSupport {

    private final UserSpotViewMapper userSpotViewMapper;
    private final UserSpotFavoriteMapper userSpotFavoriteMapper;
    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final RecommendationViewSourceClassifier recommendationViewSourceClassifier;

    public Map<Long, Double> buildUserInteractionWeights(Long userId, RecommendationAlgorithmConfigDTO config) {
        Map<Long, Double> weights = new HashMap<>();

        userSpotViewMapper.selectList(
            new LambdaQueryWrapper<UserSpotView>()
                .eq(UserSpotView::getUserId, userId)
                .select(UserSpotView::getSpotId, UserSpotView::getViewSource, UserSpotView::getViewDuration)
        ).forEach(view -> mergeInteractionWeight(weights, view.getSpotId(), calculateViewWeight(view, config)));

        userSpotFavoriteMapper.selectList(
            new LambdaQueryWrapper<UserSpotFavorite>()
                .eq(UserSpotFavorite::getUserId, userId)
                .eq(UserSpotFavorite::getIsDeleted, 0)
                .select(UserSpotFavorite::getSpotId)
        ).forEach(favorite -> mergeInteractionWeight(weights, favorite.getSpotId(), config.getWeightFavorite()));

        reviewMapper.selectList(
            new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getIsDeleted, 0)
                .select(Review::getSpotId, Review::getScore)
        ).forEach(review -> mergeInteractionWeight(
            weights,
            review.getSpotId(),
            review.getScore() * config.getWeightReviewFactor()
        ));

        orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getIsDeleted, 0)
                .in(Order::getStatus, OrderStatus.PAID.getCode(), OrderStatus.COMPLETED.getCode())
                .select(Order::getSpotId, Order::getStatus)
        ).forEach(order -> {
            double weight = order.getStatus() == OrderStatus.COMPLETED.getCode()
                ? config.getWeightOrderCompleted()
                : config.getWeightOrderPaid();
            mergeInteractionWeight(weights, order.getSpotId(), weight);
        });

        return weights;
    }

    public double calculateViewWeight(UserSpotView view, RecommendationAlgorithmConfigDTO config) {
        double baseWeight = config.getWeightView() == null ? 0.5 : config.getWeightView();
        return baseWeight
            * getViewSourceFactor(view.getViewSource(), config)
            * getViewDurationFactor(view.getViewDuration(), config);
    }

    public void mergeInteractionWeight(Map<Long, Double> weights, Long spotId, Double weight) {
        if (weights == null || spotId == null || weight == null || weight <= 0) {
            return;
        }
        weights.merge(spotId, weight, Double::sum);
    }

    private double getViewSourceFactor(String source, RecommendationAlgorithmConfigDTO config) {
        return switch (recommendationViewSourceClassifier.normalize(source)) {
            case "search" -> defaultDouble(config.getViewSourceFactorSearch(), 1.2);
            case "recommendation" -> defaultDouble(config.getViewSourceFactorRecommendation(), 1.1);
            case "home" -> defaultDouble(config.getViewSourceFactorHome(), 0.9);
            case "guide" -> defaultDouble(config.getViewSourceFactorGuide(), 1.0);
            default -> defaultDouble(config.getViewSourceFactorDetail(), 1.0);
        };
    }

    private double getViewDurationFactor(Integer duration, RecommendationAlgorithmConfigDTO config) {
        int seconds = duration == null ? 0 : Math.max(duration, 0);
        int shortThreshold = defaultInt(config.getViewDurationShortThresholdSeconds(), 10);
        int mediumThreshold = Math.max(shortThreshold, defaultInt(config.getViewDurationMediumThresholdSeconds(), 60));
        int longThreshold = Math.max(mediumThreshold, defaultInt(config.getViewDurationLongThresholdSeconds(), 180));

        if (seconds < shortThreshold) {
            return defaultDouble(config.getViewDurationFactorShort(), 0.6);
        }
        if (seconds < mediumThreshold) {
            return defaultDouble(config.getViewDurationFactorMedium(), 1.0);
        }
        if (seconds < longThreshold) {
            return defaultDouble(config.getViewDurationFactorLong(), 1.2);
        }
        return defaultDouble(config.getViewDurationFactorVeryLong(), 1.35);
    }

    private double defaultDouble(Double value, double fallback) {
        return value == null ? fallback : value;
    }

    private int defaultInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }
}
