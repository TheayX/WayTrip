package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.dto.review.request.AdminReviewListRequest;
import com.travel.dto.review.request.ReviewRequest;
import com.travel.dto.review.response.ReviewResponse;
import com.travel.dto.review.stats.SpotReviewStats;
import com.travel.dto.spot.response.SpotDetailResponse;
import com.travel.entity.Review;
import com.travel.entity.Spot;
import com.travel.entity.User;
import com.travel.mapper.ReviewMapper;
import com.travel.mapper.SpotMapper;
import com.travel.mapper.UserMapper;
import com.travel.service.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 评价服务测试
 * 重点覆盖评价提交、删除和景点评分统计刷新逻辑。
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private SpotMapper spotMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RecommendationService recommendationService;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private Review review;
    private User user;

    /**
     * 构建基础评价夹具，供删除和更新相关测试复用。
     */
    @BeforeEach
    void setUp() {
        review = new Review();
        review.setId(10L);
        review.setUserId(1L);
        review.setSpotId(100L);
        review.setScore(5);
        review.setIsDeleted(0);

        user = new User();
        user.setId(1L);
        user.setIsDeleted(0);
    }

    @Test
    void deleteReview_marksOwnReviewDeleted_andRefreshesSpotStats() {
        when(userMapper.selectById(1L)).thenReturn(user);
        when(reviewMapper.selectById(10L)).thenReturn(review);
        when(reviewMapper.selectSpotReviewStats(100L)).thenReturn(buildStats("3.5", 2L));

        reviewService.deleteReview(1L, 10L);

        assertEquals(1, review.getIsDeleted());
        verify(reviewMapper).updateById(review);
        verify(spotMapper).update(isNull(), any(UpdateWrapper.class));
    }

    @Test
    void deleteReview_rejectsDeletingOthersReview() {
        when(userMapper.selectById(2L)).thenReturn(user);
        when(reviewMapper.selectById(10L)).thenReturn(review);

        BusinessException exception = assertThrows(BusinessException.class, () -> reviewService.deleteReview(2L, 10L));

        assertEquals(ResultCode.REVIEW_DELETE_FORBIDDEN.getCode(), exception.getCode());
        verify(reviewMapper, never()).updateById(any());
        verify(spotMapper, never()).update(isNull(), any(UpdateWrapper.class));
    }

    @Test
    void deleteReview_resetsSpotStatsWhenLastReviewRemoved() {
        when(userMapper.selectById(1L)).thenReturn(user);
        when(reviewMapper.selectById(10L)).thenReturn(review);
        when(reviewMapper.selectSpotReviewStats(100L)).thenReturn(buildStats("0.0", 0L));

        reviewService.deleteReview(1L, 10L);

        ArgumentCaptor<UpdateWrapper<Spot>> wrapperCaptor = ArgumentCaptor.forClass(UpdateWrapper.class);
        verify(spotMapper).update(isNull(), wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSet();
        assertEquals("avg_rating=#{ew.paramNameValuePairs.MPGENVAL1},review_count=#{ew.paramNameValuePairs.MPGENVAL2}", sqlSegment);
    }

    @Test
    void submitReview_updatesExistingReview_andRefreshesSpotStats() {
        Spot spot = new Spot();
        spot.setId(100L);
        spot.setIsDeleted(0);
        spot.setIsPublished(1);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(spotMapper.selectById(100L)).thenReturn(spot);
        when(reviewMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(review);
        when(reviewMapper.selectSpotReviewStats(100L)).thenReturn(buildStats("4.0", 3L));

        ReviewRequest request = new ReviewRequest();
        request.setSpotId(100L);
        request.setScore(4);
        request.setComment("updated");

        reviewService.submitReview(1L, request);

        assertEquals(4, review.getScore());
        assertEquals("updated", review.getComment());
        assertEquals(0, review.getIsDeleted());
        verify(reviewMapper).updateById(review);
        verify(spotMapper).update(isNull(), any(UpdateWrapper.class));
        verify(recommendationService).invalidateUserRecommendationCache(1L);
    }

    @Test
    void submitReview_rejectsDeletedUser() {
        User deletedUser = new User();
        deletedUser.setId(1L);
        deletedUser.setIsDeleted(1);
        when(userMapper.selectById(1L)).thenReturn(deletedUser);

        ReviewRequest request = new ReviewRequest();
        request.setSpotId(100L);
        request.setScore(4);

        BusinessException exception = assertThrows(BusinessException.class, () -> reviewService.submitReview(1L, request));

        assertEquals(ResultCode.TOKEN_INVALID.getCode(), exception.getCode());
        verify(spotMapper, never()).selectById(any());
    }

    @Test
    void getUserReview_returnsUnknownNicknameWhenAuthorDeleted() {
        Review deletedAuthorReview = new Review();
        deletedAuthorReview.setId(20L);
        deletedAuthorReview.setUserId(1L);
        deletedAuthorReview.setSpotId(100L);
        deletedAuthorReview.setScore(4);
        deletedAuthorReview.setComment("保留历史评价");
        deletedAuthorReview.setIsDeleted(0);

        User deletedUser = new User();
        deletedUser.setId(1L);
        deletedUser.setIsDeleted(1);

        when(userMapper.selectById(1L)).thenReturn(user, deletedUser);
        when(reviewMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(deletedAuthorReview);

        ReviewResponse response = reviewService.getUserReview(1L, 100L);

        assertEquals("未知用户", response.getNickname());
        assertNull(response.getAvatar());
    }

    @Test
    void getSpotReviews_returnsUnknownNicknameWhenAuthorHardDeleted() {
        Review purgedAuthorReview = new Review();
        purgedAuthorReview.setId(21L);
        purgedAuthorReview.setUserId(9L);
        purgedAuthorReview.setSpotId(100L);
        purgedAuthorReview.setScore(5);
        purgedAuthorReview.setComment("历史评价");
        purgedAuthorReview.setIsDeleted(0);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Review> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        page.setRecords(java.util.List.of(purgedAuthorReview));
        page.setTotal(1L);

        when(reviewMapper.selectReviewPage(any(), eq(100L))).thenReturn(page);
        when(userMapper.selectById(9L)).thenReturn(null);
        when(spotMapper.selectById(100L)).thenReturn(null);

        var result = reviewService.getSpotReviews(100L, 1, 10);

        assertEquals("未知用户", result.getList().get(0).getNickname());
        assertNull(result.getList().get(0).getAvatar());
    }

    @Test
    void getUserReviews_keepsDefaultOrderingPathWithoutSortParameters() {
        Review ownReview = new Review();
        ownReview.setId(22L);
        ownReview.setUserId(1L);
        ownReview.setSpotId(100L);
        ownReview.setScore(5);
        ownReview.setComment("我的评价");
        ownReview.setSpotName("西湖");
        ownReview.setCoverImageUrl("/uploads/spot/xihu.jpg");
        ownReview.setIsDeleted(0);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Review> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        page.setRecords(java.util.List.of(ownReview));
        page.setTotal(1L);

        when(userMapper.selectById(1L)).thenReturn(user);
        when(reviewMapper.selectUserReviewPage(any(), eq(1L))).thenReturn(page);

        var result = reviewService.getUserReviews(1L, 1, 10);

        assertEquals(1L, result.getTotal());
        assertEquals("西湖", result.getList().get(0).getSpotName());
        verify(reviewMapper).selectUserReviewPage(any(), eq(1L));
    }

    @Test
    void getAdminReviews_keepsJoinedSpotNameWhenEntityIsNotQueried() {
        Review adminReview = new Review();
        adminReview.setId(30L);
        adminReview.setUserId(1L);
        adminReview.setSpotId(100L);
        adminReview.setScore(5);
        adminReview.setComment("后台评价记录");
        adminReview.setSpotName("西湖");
        adminReview.setCoverImageUrl("/uploads/spot/xihu.jpg");
        adminReview.setIsDeleted(0);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Review> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        page.setRecords(java.util.List.of(adminReview));
        page.setTotal(1L);

        when(reviewMapper.selectAdminReviewPage(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(page);

        var request = new AdminReviewListRequest();
        request.setPage(1);
        request.setPageSize(10);

        var result = reviewService.getAdminReviews(request);

        assertEquals("西湖", result.getList().get(0).getSpotName());
        assertEquals("/uploads/spot/xihu.jpg", result.getList().get(0).getCoverImageUrl());
    }

    @Test
    void getAdminReviews_acceptsScoreAndDateFilters() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Review> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        page.setRecords(java.util.List.of());
        page.setTotal(0L);

        when(reviewMapper.selectAdminReviewPage(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(page);

        AdminReviewListRequest request = new AdminReviewListRequest();
        request.setPage(1);
        request.setPageSize(10);
        request.setMinScore(1);
        request.setMaxScore(2);
        request.setStartDate(java.time.LocalDate.of(2026, 1, 1));
        request.setEndDate(java.time.LocalDate.of(2026, 1, 31));

        var result = reviewService.getAdminReviews(request);

        assertEquals(0L, result.getTotal());
        verify(reviewMapper).selectAdminReviewPage(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    /**
     * 构造评分统计结果，模拟聚合查询返回值。
     */
    private SpotReviewStats buildStats(String avgRating, Long reviewCount) {
        SpotReviewStats stats = new SpotReviewStats();
        stats.setAvgRating(new java.math.BigDecimal(avgRating));
        stats.setReviewCount(reviewCount);
        return stats;
    }
}
