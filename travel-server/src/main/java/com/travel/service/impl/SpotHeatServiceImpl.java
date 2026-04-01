package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.constant.SpotHeatLevelConstants;
import com.travel.dto.recommendation.model.RecommendationConfigBundleDTO;
import com.travel.entity.Order;
import com.travel.entity.Review;
import com.travel.entity.Spot;
import com.travel.entity.UserSpotFavorite;
import com.travel.entity.UserSpotView;
import com.travel.enums.OrderStatus;
import com.travel.mapper.OrderMapper;
import com.travel.mapper.ReviewMapper;
import com.travel.mapper.SpotMapper;
import com.travel.mapper.UserSpotFavoriteMapper;
import com.travel.mapper.UserSpotViewMapper;
import com.travel.service.RecommendationService;
import com.travel.service.SpotHeatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 景点热度服务实现，负责热度分重算和批量同步。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpotHeatServiceImpl implements SpotHeatService {

    private final SpotMapper spotMapper;
    private final UserSpotFavoriteMapper userSpotFavoriteMapper;
    private final ReviewMapper reviewMapper;
    private final UserSpotViewMapper userSpotViewMapper;
    private final OrderMapper orderMapper;
    private final RecommendationService recommendationService;

    @Override
    @Transactional
    public void refreshSpotHeat(Long spotId) {
        Spot spot = getActiveSpot(spotId);
        applyHeatScore(spot);
    }

    @Override
    @Transactional
    public void refreshAllSpotHeat() {
        List<Spot> spots = spotMapper.selectList(
            new LambdaQueryWrapper<Spot>()
                .eq(Spot::getIsDeleted, 0)
                .select(Spot::getId, Spot::getHeatLevel)
        );

        for (Spot spot : spots) {
            applyHeatScore(spot);
        }
    }

    private void applyHeatScore(Spot spot) {
        RecommendationConfigBundleDTO config = recommendationService.getConfig();
        int totalHeatScore = SpotHeatLevelConstants.toBaseScore(spot.getHeatLevel())
            + calculateBehaviorHeatScore(spot.getId(), config);
        spotMapper.update(
            null,
            new UpdateWrapper<Spot>()
                .eq("id", spot.getId())
                .set("heat_score", totalHeatScore)
        );
        log.info("景点热度同步完成: spotId={}, heatLevel={}, heatScore={}", spot.getId(), spot.getHeatLevel(), totalHeatScore);
    }

    /**
     * 单景点热度刷新要求景点仍然有效，统一收口存在性校验。
     */
    private Spot getActiveSpot(Long spotId) {
        Spot spot = spotMapper.selectById(spotId);
        if (spot == null || spot.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.SPOT_NOT_FOUND);
        }
        return spot;
    }

    private int calculateBehaviorHeatScore(Long spotId, RecommendationConfigBundleDTO config) {
        long viewCount = userSpotViewMapper.selectCount(
            new LambdaQueryWrapper<UserSpotView>().eq(UserSpotView::getSpotId, spotId)
        );
        long favoriteCount = userSpotFavoriteMapper.selectCount(
            new LambdaQueryWrapper<UserSpotFavorite>()
                .eq(UserSpotFavorite::getSpotId, spotId)
                .eq(UserSpotFavorite::getIsDeleted, 0)
        );
        long reviewCount = reviewMapper.selectCount(
            new LambdaQueryWrapper<Review>()
                .eq(Review::getSpotId, spotId)
                .eq(Review::getIsDeleted, 0)
        );
        long paidOrderCount = orderMapper.selectCount(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getSpotId, spotId)
                .eq(Order::getIsDeleted, 0)
                .in(Order::getStatus, OrderStatus.PAID.getCode(), OrderStatus.COMPLETED.getCode())
        );
        long completedOrderCount = orderMapper.selectCount(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getSpotId, spotId)
                .eq(Order::getIsDeleted, 0)
                .eq(Order::getStatus, OrderStatus.COMPLETED.getCode())
        );

        int viewIncrement = positiveOrDefault(config.getHeat().getHeatViewIncrement(), 1);
        int favoriteIncrement = positiveOrDefault(config.getHeat().getHeatFavoriteIncrement(), 3);
        int reviewIncrement = positiveOrDefault(config.getHeat().getHeatReviewIncrement(), 2);
        int paidIncrement = positiveOrDefault(config.getHeat().getHeatOrderPaidIncrement(), 5);
        int completedIncrement = positiveOrDefault(config.getHeat().getHeatOrderCompletedIncrement(), 8);

        return Math.toIntExact(
            viewCount * (long) viewIncrement
                + favoriteCount * (long) favoriteIncrement
                + reviewCount * (long) reviewIncrement
                + paidOrderCount * (long) paidIncrement
                + completedOrderCount * (long) completedIncrement
        );
    }

    private int positiveOrDefault(Integer value, int fallback) {
        return value != null && value > 0 ? value : fallback;
    }
}
