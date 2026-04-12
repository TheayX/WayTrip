package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.constant.ResourceDisplayText;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.PageResult;
import com.travel.common.result.ResultCode;
import com.travel.dto.review.request.AdminReviewListRequest;
import com.travel.dto.review.request.ReviewFeedRequest;
import com.travel.dto.review.request.ReviewRequest;
import com.travel.dto.review.response.ReviewResponse;
import com.travel.dto.review.stats.SpotRatingStats;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 评价服务实现，负责评价提交、查询与景点评分同步。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    // 持久层依赖
    private final ReviewMapper reviewMapper;
    private final SpotMapper spotMapper;
    private final UserMapper userMapper;
    private final RecommendationService recommendationService;

    // 时间格式配置
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // 用户端评价操作

    @Override
    @Transactional
    public void submitReview(Long userId, ReviewRequest request) {
        getActiveUser(userId);
        Spot spot = getAvailableSpot(request.getSpotId());
        if (spot.getIsPublished() != 1) {
            throw new BusinessException(ResultCode.SPOT_OFFLINE);
        }

        Review existingReview = reviewMapper.selectOne(
            new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getSpotId, request.getSpotId())
        );

        if (existingReview != null) {
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
        }

        // 评价变更后需要立即刷新景点评分聚合，并清理推荐缓存。
        updateSpotAvgRating(request.getSpotId());
        recommendationService.invalidateUserRecommendationCache(userId);
        log.info("用户提交评价: userId={}, spotId={}, score={}", userId, request.getSpotId(), request.getScore());
    }

    @Override
    public ReviewResponse getUserReview(Long userId, Long spotId) {
        getActiveUser(userId);
        Review review = reviewMapper.selectOne(
            new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getSpotId, spotId)
                .eq(Review::getIsDeleted, 0)
        );

        if (review == null) {
            return null;
        }

        return convertToResponse(review, false);
    }

    @Override
    public PageResult<ReviewResponse> getSpotReviews(Long spotId, Integer page, Integer pageSize) {
        Page<Review> pageObj = new Page<>(page, pageSize);
        pageObj = (Page<Review>) reviewMapper.selectReviewPage(pageObj, spotId);

        List<ReviewResponse> list = pageObj.getRecords().stream()
            .map(review -> convertToResponse(review, false))
            .collect(Collectors.toList());

        return PageResult.of(list, pageObj.getTotal(), page, pageSize);
    }

    @Override
    public PageResult<ReviewResponse> getReviewFeed(ReviewFeedRequest request) {
        int minScore = "negative".equals(request.getType()) ? 0 : 4;
        int maxScore = "negative".equals(request.getType()) ? 2 : 5;

        Page<Review> pageObj = new Page<>(request.getPage(), request.getPageSize());
        pageObj = (Page<Review>) reviewMapper.selectReviewFeedPage(pageObj, minScore, maxScore);

        List<ReviewResponse> list = pageObj.getRecords().stream()
                .map(review -> convertToResponse(review, false))
                .collect(Collectors.toList());

        return PageResult.of(list, pageObj.getTotal(), request.getPage(), request.getPageSize());
    }

    @Override
    public PageResult<ReviewResponse> getUserReviews(Long userId, Integer page, Integer pageSize) {
        getActiveUser(userId);
        Page<Review> pageObj = new Page<>(page, pageSize);
        pageObj = (Page<Review>) reviewMapper.selectUserReviewPage(pageObj, userId);

        List<ReviewResponse> list = pageObj.getRecords().stream()
            .map(review -> convertToResponse(review, false))
            .collect(Collectors.toList());

        return PageResult.of(list, pageObj.getTotal(), page, pageSize);
    }

    @Override
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        getActiveUser(userId);
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

    // 管理端评价查询与评分刷新、删除

    @Override
    public PageResult<ReviewResponse> getAdminReviews(AdminReviewListRequest request) {
        Page<Review> pageObj = new Page<>(request.getPage(), request.getPageSize());
        pageObj = (Page<Review>) reviewMapper.selectAdminReviewPage(pageObj, request.getNickname(), request.getSpotName());

        List<ReviewResponse> list = pageObj.getRecords().stream()
            .map(review -> convertToResponse(review, true))
            .collect(Collectors.toList());

        return PageResult.of(list, pageObj.getTotal(), request.getPage(), request.getPageSize());
    }

    @Override
    public int getUserReviewCount(Long userId) {
        getActiveUser(userId);
        return Math.toIntExact(reviewMapper.selectCount(
            new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getIsDeleted, 0)
        ));
    }

    @Override
    @Transactional
    public void refreshSpotRating(Long spotId) {
        getAvailableSpot(spotId);
        updateSpotAvgRating(spotId);
        recommendationService.invalidateGlobalRecommendationCaches();
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
        recommendationService.invalidateGlobalRecommendationCaches();
    }

    @Override
    @Transactional
    public void deleteReviewByAdmin(Long reviewId) {
        Review review = getActiveReview(reviewId);

        review.setIsDeleted(1);
        reviewMapper.updateById(review);
        updateSpotAvgRating(review.getSpotId());
        recommendationService.invalidateUserRecommendationCache(review.getUserId());
        log.info("管理员删除评价: reviewId={}, userId={}, spotId={}", reviewId, review.getUserId(), review.getSpotId());
    }

    // 景点评分同步与响应转换

    /**
     * 评价读写不再依赖物理外键兜底，先校验用户仍然处于有效状态。
     */
    private User getActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
        return user;
    }

    /**
     * 评价相关操作统一要求景点处于有效状态，避免不同入口各自散落相同校验。
     */
    private Spot getAvailableSpot(Long spotId) {
        Spot spot = spotMapper.selectById(spotId);
        if (spot == null || spot.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.SPOT_NOT_FOUND);
        }
        return spot;
    }

    /**
     * 管理端删除和用户删除都要求评价仍然有效，统一收口存在性判断。
     */
    private Review getActiveReview(Long reviewId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null || review.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.REVIEW_NOT_FOUND);
        }
        return review;
    }

    private void updateSpotAvgRating(Long spotId) {
        SpotRatingStats stats = reviewMapper.selectSpotRatingStats(spotId);
        BigDecimal avgRating = stats != null && stats.getAvgRating() != null
            ? stats.getAvgRating()
            : BigDecimal.ZERO;
        long ratingCount = stats != null && stats.getRatingCount() != null
            ? stats.getRatingCount()
            : 0L;

        spotMapper.update(
            null,
            new UpdateWrapper<Spot>()
                .eq("id", spotId)
                .set("avg_rating", avgRating)
                .set("rating_count", ratingCount)
        );
    }

    // 响应对象转换方法
    private ReviewResponse convertToResponse(Review review, boolean adminView) {
        User user = null;
        if (review.getNickname() == null || review.getAvatarUrl() == null) {
            user = userMapper.selectById(review.getUserId());
        }
        Spot spot = null;
        if (review.getSpotName() == null || review.getCoverImageUrl() == null) {
            spot = spotMapper.selectById(review.getSpotId());
        }

        boolean isActiveUser = user != null && user.getIsDeleted() != null && user.getIsDeleted() == 0;
        String nickname = review.getNickname() != null
            ? review.getNickname()
            : resolveUserDisplayNickname(user, adminView);
        String avatar = review.getAvatarUrl() != null
            ? review.getAvatarUrl()
            : (isActiveUser ? user.getAvatarUrl() : null);
        String spotName = review.getSpotName() != null
            ? resolveSpotDisplayName(spot, review.getSpotName(), adminView)
            : resolveSpotDisplayName(spot, null, adminView);
        String coverImageUrl = review.getCoverImageUrl() != null
            ? review.getCoverImageUrl()
            : (spot != null ? spot.getCoverImageUrl() : null);

        return ReviewResponse.builder()
            .id(review.getId())
            .userId(review.getUserId())
            .spotId(review.getSpotId())
            .spotName(spotName)
            .coverImageUrl(coverImageUrl)
            .score(review.getScore())
            .comment(review.getComment())
            .nickname(nickname)
            .avatar(avatar)
            .createdAt(review.getCreatedAt() != null ? review.getCreatedAt().format(DATE_FORMATTER) : null)
            .updatedAt(review.getUpdatedAt() != null ? review.getUpdatedAt().format(DATE_FORMATTER) : null)
            .build();
    }

    /**
     * 历史评价需要区分“账号已注销”和“用户记录已被硬删”，避免展示语义失真。
     */
    private String resolveUserDisplayNickname(User user, boolean adminView) {
        if (user == null) {
            return adminView ? ResourceDisplayText.User.PURGED : ResourceDisplayText.User.UNKNOWN;
        }
        if (user.getIsDeleted() != null && user.getIsDeleted() == 1) {
            return adminView ? ResourceDisplayText.User.DEACTIVATED : ResourceDisplayText.User.UNKNOWN;
        }
        return user.getNickname();
    }

    /**
     * 历史评价保留时，景点失效语义要比原始名称更重要，避免误导运营判断。
     */
    private String resolveSpotDisplayName(Spot spot, String currentSpotName, boolean adminView) {
        if (spot == null) {
            return adminView ? ResourceDisplayText.Spot.PURGED : ResourceDisplayText.Spot.UNKNOWN;
        }
        if (spot.getIsDeleted() != null && spot.getIsDeleted() == 1) {
            return adminView ? ResourceDisplayText.Spot.DELETED : ResourceDisplayText.Spot.UNKNOWN;
        }
        if (spot.getIsPublished() != null && spot.getIsPublished() != 1) {
            return adminView ? ResourceDisplayText.Spot.OFFLINE : ResourceDisplayText.Spot.UNKNOWN;
        }
        return currentSpotName != null ? currentSpotName : spot.getName();
    }

}
