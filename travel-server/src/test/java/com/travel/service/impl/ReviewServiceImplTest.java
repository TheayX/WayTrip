package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.entity.Review;
import com.travel.entity.Spot;
import com.travel.mapper.ReviewMapper;
import com.travel.mapper.SpotMapper;
import com.travel.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private SpotMapper spotMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private Review review;

    @BeforeEach
    void setUp() {
        review = new Review();
        review.setId(10L);
        review.setUserId(1L);
        review.setSpotId(100L);
        review.setScore(5);
        review.setIsDeleted(0);
    }

    @Test
    void deleteReview_marksOwnReviewDeleted_andRefreshesSpotStats() {
        when(reviewMapper.selectById(10L)).thenReturn(review);
        when(reviewMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(buildReview(3), buildReview(4)));

        reviewService.deleteReview(1L, 10L);

        assertEquals(1, review.getIsDeleted());
        verify(reviewMapper).updateById(review);
        verify(spotMapper).update(isNull(), any(UpdateWrapper.class));
    }

    @Test
    void deleteReview_rejectsDeletingOthersReview() {
        when(reviewMapper.selectById(10L)).thenReturn(review);

        BusinessException exception = assertThrows(BusinessException.class, () -> reviewService.deleteReview(2L, 10L));

        assertEquals(ResultCode.REVIEW_DELETE_FORBIDDEN.getCode(), exception.getCode());
        verify(reviewMapper, never()).updateById(any());
        verify(spotMapper, never()).update(isNull(), any(UpdateWrapper.class));
    }

    @Test
    void deleteReview_resetsSpotStatsWhenLastReviewRemoved() {
        when(reviewMapper.selectById(10L)).thenReturn(review);
        when(reviewMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        reviewService.deleteReview(1L, 10L);

        ArgumentCaptor<UpdateWrapper<Spot>> wrapperCaptor = ArgumentCaptor.forClass(UpdateWrapper.class);
        verify(spotMapper).update(isNull(), wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSet();
        assertEquals("avg_rating=#{ew.paramNameValuePairs.MPGENVAL1},rating_count=#{ew.paramNameValuePairs.MPGENVAL2}", sqlSegment);
    }

    private Review buildReview(int score) {
        Review item = new Review();
        item.setScore(score);
        item.setSpotId(100L);
        item.setIsDeleted(0);
        return item;
    }
}
