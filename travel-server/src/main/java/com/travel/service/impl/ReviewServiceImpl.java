package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.constant.ResourceDisplayText;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.PageResult;
import com.travel.common.result.ResultCode;
import com.travel.dto.review.request.AdminReviewListRequest;
import com.travel.dto.review.request.ReviewFeedRequest;
import com.travel.dto.review.request.ReviewRequest;
import com.travel.dto.review.response.ReviewResponse;
import com.travel.dto.review.stats.SpotReviewStats;
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

    /**
     * 提交或更新用户对景点的评价。
     */
    @Override
    @Transactional
    public void submitReview(Long userId, ReviewRequest request) {
        getActiveUser(userId);
        Spot spot = getAvailableSpot(request.getSpotId());
        if (spot.getIsPublished() != 1) {
            throw new BusinessException(ResultCode.SPOT_OFFLINE);
        }

        // 同一用户对同一景点只保留一条有效评价，重复提交走覆盖更新。
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

    /**
     * 获取用户自己对指定景点的评价。
     */
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

    /**
     * 分页获取某个景点下的公开评价列表。
     */
    @Override
    public PageResult<ReviewResponse> getSpotReviews(Long spotId, Integer page, Integer pageSize) {
        Page<Review> pageObj = new Page<>(page, pageSize);
        pageObj = (Page<Review>) reviewMapper.selectReviewPage(pageObj, spotId);

        List<ReviewResponse> list = pageObj.getRecords().stream()
            .map(review -> convertToResponse(review, false))
            .collect(Collectors.toList());

        return PageResult.of(list, pageObj.getTotal(), page, pageSize);
    }

    /**
     * 获取评价广场列表，按正向或负向评分区间筛选。
     */
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

    /**
     * 获取当前用户发表过的评价列表。
     */
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

    /**
     * 删除用户自己的评价，并同步刷新景点评分聚合。
     */
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

    /**
     * 获取管理端评价列表，支持条件筛选与排序。
     */
    @Override
    public PageResult<ReviewResponse> getAdminReviews(AdminReviewListRequest request) {
        Page<Review> pageObj = new Page<>(request.getPage(), request.getPageSize());
        pageObj = (Page<Review>) reviewMapper.selectAdminReviewPage(
            pageObj,
            request.getNickname(),
            request.getSpotName(),
            request.getMinScore(),
            request.getMaxScore(),
            request.getStartDate() != null ? request.getStartDate().toString() : null,
            request.getEndDate() != null ? request.getEndDate().toString() : null,
            resolveAdminReviewSortColumn(request.getSortBy()),
            resolveAdminReviewSortOrder(request.getSortOrder())
        );

        List<ReviewResponse> list = pageObj.getRecords().stream()
            .map(review -> convertToResponse(review, true))
            .collect(Collectors.toList());

        return PageResult.of(list, pageObj.getTotal(), request.getPage(), request.getPageSize());
    }

    /**
     * 获取用户当前有效评价数量。
     */
    @Override
    public int getUserReviewCount(Long userId) {
        getActiveUser(userId);
        return Math.toIntExact(reviewMapper.selectCount(
            new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getIsDeleted, 0)
        ));
    }

    /**
     * 刷新单个景点的平均评分与评价数聚合。
     */
    @Override
    @Transactional
    public void refreshSpotRating(Long spotId) {
        getAvailableSpot(spotId);
        updateSpotAvgRating(spotId);
        recommendationService.invalidateGlobalRecommendationCaches();
    }

    /**
     * 刷新全部有效景点的评分聚合。
     */
    @Override
    @Transactional
    public void refreshAllSpotRatings() {
        List<Spot> spots = spotMapper.selectList(
            new LambdaQueryWrapper<Spot>()
                .eq(Spot::getIsDeleted, 0)
                .select(Spot::getId)
        );

        // 全量刷新按景点逐个重算，保证聚合口径与单点评分刷新保持一致。
        for (Spot spot : spots) {
            updateSpotAvgRating(spot.getId());
        }
        recommendationService.invalidateGlobalRecommendationCaches();
    }

    /**
     * 管理员删除指定评价。
     */
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

    private String resolveAdminReviewSortColumn(String sortBy) {
        if ("id".equals(sortBy)) {
            return "r.id";
        }
        if ("score".equals(sortBy)) {
            return "r.score";
        }
        if ("createdAt".equals(sortBy)) {
            return "r.created_at";
        }
        if ("updatedAt".equals(sortBy)) {
            return "r.updated_at";
        }
        return null;
    }

    private String resolveAdminReviewSortOrder(String sortOrder) {
        return "asc".equalsIgnoreCase(sortOrder) ? "ASC" : "DESC";
    }

    /**
     * 按当前有效评价重新回写景点平均分和评价数。
     */
    private void updateSpotAvgRating(Long spotId) {
        SpotReviewStats stats = reviewMapper.selectSpotReviewStats(spotId);
        BigDecimal avgRating = stats != null && stats.getAvgRating() != null
            ? stats.getAvgRating()
            : BigDecimal.ZERO;
        long reviewCount = stats != null && stats.getReviewCount() != null
            ? stats.getReviewCount()
            : 0L;

        spotMapper.update(
            null,
            new UpdateWrapper<Spot>()
                .eq("id", spotId)
                .set("avg_rating", avgRating)
                .set("review_count", reviewCount)
        );
    }

    // 响应对象转换方法
    private ReviewResponse convertToResponse(Review review, boolean adminView) {
        // 优先复用联表结果中的快照字段，缺失时再补查用户和景点，减少不必要查询。
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
        // 联表已经带回景点名称时，优先信任当前快照，避免实体未补查时被误判成“已清除景点”。
        if (currentSpotName != null && spot == null) {
            return currentSpotName;
        }
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

