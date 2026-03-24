package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.PageResult;
import com.travel.common.result.ResultCode;
import com.travel.dto.review.AdminReviewListRequest;
import com.travel.dto.review.ReviewRequest;
import com.travel.dto.review.ReviewResponse;
import com.travel.entity.Review;
import com.travel.entity.Spot;
import com.travel.entity.User;
import com.travel.mapper.ReviewMapper;
import com.travel.mapper.SpotMapper;
import com.travel.mapper.UserMapper;
import com.travel.service.RecommendationService;
import com.travel.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final int REVIEW_HEAT_INCREMENT = 2;

    private final ReviewMapper reviewMapper;
    private final SpotMapper spotMapper;
    private final UserMapper userMapper;
    private final RecommendationService recommendationService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    @Transactional
    public void submitReview(Long userId, ReviewRequest request) {
        Spot spot = spotMapper.selectById(request.getSpotId());
        if (spot == null || spot.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.SPOT_NOT_FOUND);
        }
        if (spot.getIsPublished() != 1) {
            throw new BusinessException(ResultCode.SPOT_OFFLINE);
        }

        Review existingReview = reviewMapper.selectOne(
            new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getSpotId, request.getSpotId())
        );

        boolean shouldIncreaseHeat = false;
        if (existingReview != null) {
            shouldIncreaseHeat = existingReview.getIsDeleted() != null && existingReview.getIsDeleted() == 1;
            existingReview.setScore(request.getScore());
            existingReview.setComment(request.getComment());
            existingReview.setIsDeleted(0);
            reviewMapper.updateById(existingReview);
        } else {
            Review review = new Review();
            review.setUserId(userId);
            review.setSpotId(request.getSpotId());
            review.setScore(request.getScore());
            review.setComment(request.getComment());
            reviewMapper.insert(review);
            shouldIncreaseHeat = true;
        }

        if (shouldIncreaseHeat) {
            incrementHeatScore(request.getSpotId(), REVIEW_HEAT_INCREMENT);
        }

        updateSpotAvgRating(request.getSpotId());
        recommendationService.invalidateUserRecommendationCache(userId);
        log.info("用户提交评价: userId={}, spotId={}, score={}", userId, request.getSpotId(), request.getScore());
    }

    @Override
    public ReviewResponse getUserReview(Long userId, Long spotId) {
        Review review = reviewMapper.selectOne(
            new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getSpotId, spotId)
                .eq(Review::getIsDeleted, 0)
        );

        if (review == null) {
            return null;
        }

        return convertToResponse(review);
    }

    @Override
    public PageResult<ReviewResponse> getSpotReviews(Long spotId, Integer page, Integer pageSize) {
        Page<Review> pageObj = new Page<>(page, pageSize);
        pageObj = (Page<Review>) reviewMapper.selectReviewPage(pageObj, spotId);

        List<ReviewResponse> list = pageObj.getRecords().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());

        return PageResult.of(list, pageObj.getTotal(), page, pageSize);
    }

    @Override
    public PageResult<ReviewResponse> getUserReviews(Long userId, Integer page, Integer pageSize) {
        Page<Review> pageObj = new Page<>(page, pageSize);
        pageObj = (Page<Review>) reviewMapper.selectUserReviewPage(pageObj, userId);

        List<ReviewResponse> list = pageObj.getRecords().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());

        return PageResult.of(list, pageObj.getTotal(), page, pageSize);
    }

    @Override
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null || review.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.REVIEW_NOT_FOUND);
        }
        if (!review.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.REVIEW_DELETE_FORBIDDEN);
        }

        review.setIsDeleted(1);
        reviewMapper.updateById(review);
        updateSpotAvgRating(review.getSpotId());
        recommendationService.invalidateUserRecommendationCache(userId);
        log.info("用户删除评价: userId={}, reviewId={}, spotId={}", userId, reviewId, review.getSpotId());
    }

    @Override
    public PageResult<ReviewResponse> getAdminReviews(AdminReviewListRequest request) {
        Page<Review> pageObj = new Page<>(request.getPage(), request.getPageSize());
        pageObj = (Page<Review>) reviewMapper.selectAdminReviewPage(pageObj, request.getNickname(), request.getSpotName());

        List<ReviewResponse> list = pageObj.getRecords().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());

        return PageResult.of(list, pageObj.getTotal(), request.getPage(), request.getPageSize());
    }

    @Override
    public int getUserReviewCount(Long userId) {
        return Math.toIntExact(reviewMapper.selectCount(
            new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getIsDeleted, 0)
        ));
    }

    @Override
    @Transactional
    public void refreshSpotRating(Long spotId) {
        Spot spot = spotMapper.selectById(spotId);
        if (spot == null || spot.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.SPOT_NOT_FOUND);
        }
        updateSpotAvgRating(spotId);
    }

    @Override
    @Transactional
    public void refreshAllSpotRatings() {
        List<Spot> spots = spotMapper.selectList(
            new LambdaQueryWrapper<Spot>()
                .eq(Spot::getIsDeleted, 0)
                .select(Spot::getId)
        );

        for (Spot spot : spots) {
            updateSpotAvgRating(spot.getId());
        }
    }

    private void updateSpotAvgRating(Long spotId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getSpotId, spotId);
        wrapper.eq(Review::getIsDeleted, 0);
        List<Review> reviews = reviewMapper.selectList(wrapper);

        if (reviews.isEmpty()) {
            spotMapper.update(
                null,
                new UpdateWrapper<Spot>()
                    .eq("id", spotId)
                    .set("avg_rating", BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP))
                    .set("rating_count", 0)
            );
            return;
        }

        double avg = reviews.stream()
            .mapToInt(Review::getScore)
            .average()
            .orElse(0);

        BigDecimal avgRating = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP);

        spotMapper.update(
            null,
            new UpdateWrapper<Spot>()
                .eq("id", spotId)
                .set("avg_rating", avgRating)
                .set("rating_count", reviews.size())
        );
    }

    private ReviewResponse convertToResponse(Review review) {
        User user = null;
        if (review.getNickname() == null || review.getAvatarUrl() == null) {
            user = userMapper.selectById(review.getUserId());
        }

        String nickname = review.getNickname() != null
            ? review.getNickname()
            : (user != null ? user.getNickname() : "匿名用户");
        String avatar = review.getAvatarUrl() != null
            ? review.getAvatarUrl()
            : (user != null ? user.getAvatarUrl() : null);

        return ReviewResponse.builder()
            .id(review.getId())
            .userId(review.getUserId())
            .spotId(review.getSpotId())
            .spotName(review.getSpotName())
            .coverImageUrl(review.getCoverImageUrl())
            .score(review.getScore())
            .comment(review.getComment())
            .nickname(nickname)
            .avatar(avatar)
            .createdAt(review.getCreatedAt() != null ? review.getCreatedAt().format(DATE_FORMATTER) : null)
            .updatedAt(review.getUpdatedAt() != null ? review.getUpdatedAt().format(DATE_FORMATTER) : null)
            .build();
    }

    private void incrementHeatScore(Long spotId, int delta) {
        spotMapper.update(
            null,
            new UpdateWrapper<Spot>()
                .eq("id", spotId)
                .setSql("heat_score = COALESCE(heat_score, 0) + " + delta)
        );
    }
}
