package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.PageResult;
import com.travel.common.result.ResultCode;
import com.travel.dto.review.ReviewRequest;
import com.travel.dto.review.ReviewResponse;
import com.travel.entity.Review;
import com.travel.entity.Spot;
import com.travel.entity.User;
import com.travel.mapper.ReviewMapper;
import com.travel.mapper.SpotMapper;
import com.travel.mapper.UserMapper;
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

/**
 * 评价服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final SpotMapper spotMapper;
    private final UserMapper userMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    @Transactional
    public void submitReview(Long userId, ReviewRequest request) {
        // 检查景点是否存在
        Spot spot = spotMapper.selectById(request.getSpotId());
        if (spot == null || spot.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.SPOT_NOT_FOUND);
        }
        if (spot.getIsPublished() != 1) {
            throw new BusinessException(ResultCode.SPOT_OFFLINE);
        }

        // 查找是否已有评价
        Review existingReview = reviewMapper.selectOne(
            new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getSpotId, request.getSpotId())
        );

        if (existingReview != null) {
            // 更新评价
            existingReview.setScore(request.getScore());
            existingReview.setComment(request.getComment());
            existingReview.setIsDeleted(0);
            reviewMapper.updateById(existingReview);
        } else {
            // 新增评价
            Review review = new Review();
            review.setUserId(userId);
            review.setSpotId(request.getSpotId());
            review.setScore(request.getScore());
            review.setComment(request.getComment());
            reviewMapper.insert(review);
        }

        // 更新景点平均评分
        updateSpotAvgRating(request.getSpotId());
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
    public int getUserReviewCount(Long userId) {
        return Math.toIntExact(reviewMapper.selectCount(
            new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getIsDeleted, 0)
        ));
    }

    private void updateSpotAvgRating(Long spotId) {
        // 计算平均评分
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getSpotId, spotId);
        wrapper.eq(Review::getIsDeleted, 0);
        List<Review> reviews = reviewMapper.selectList(wrapper);

        if (reviews.isEmpty()) {
            return;
        }

        double avg = reviews.stream()
                .mapToInt(Review::getScore)
                .average()
                .orElse(0);

        BigDecimal avgRating = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP);

        // 更新景点评分统计（不更新 updatedAt）
        spotMapper.update(
            null,
            new UpdateWrapper<Spot>()
                .eq("id", spotId)
                .set("avg_rating", avgRating)
                .set("rating_count", reviews.size())
        );
    }

    private ReviewResponse convertToResponse(Review review) {
        User user = userMapper.selectById(review.getUserId());

        return ReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUserId())
                .spotId(review.getSpotId())
                .score(review.getScore())
                .comment(review.getComment())
                .nickname(user != null ? user.getNickname() : "匿名用户")
                .avatar(user != null ? user.getAvatarUrl() : null)
                .createdAt(review.getCreatedAt() != null ? review.getCreatedAt().format(DATE_FORMATTER) : null)
                .build();
    }
}

