package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.PageResult;
import com.travel.common.result.ResultCode;
import com.travel.dto.rating.RatingRequest;
import com.travel.dto.rating.RatingResponse;
import com.travel.entity.Rating;
import com.travel.entity.Spot;
import com.travel.entity.User;
import com.travel.mapper.RatingMapper;
import com.travel.mapper.SpotMapper;
import com.travel.mapper.UserMapper;
import com.travel.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 评分服务实现
 */
@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {

    private final RatingMapper ratingMapper;
    private final SpotMapper spotMapper;
    private final UserMapper userMapper;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    @Transactional
    public void submitRating(Long userId, RatingRequest request) {
        // 检查景点是否存在
        Spot spot = spotMapper.selectById(request.getSpotId());
        if (spot == null || spot.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.SPOT_NOT_FOUND);
        }
        if (spot.getPublished() != 1) {
            throw new BusinessException(ResultCode.SPOT_OFFLINE);
        }
        
        // 查找是否已有评分
        Rating existingRating = ratingMapper.selectOne(
            new LambdaQueryWrapper<Rating>()
                .eq(Rating::getUserId, userId)
                .eq(Rating::getSpotId, request.getSpotId())
        );
        
        if (existingRating != null) {
            // 更新评分
            existingRating.setScore(request.getScore());
            existingRating.setComment(request.getComment());
            existingRating.setIsDeleted(0);
            ratingMapper.updateById(existingRating);
        } else {
            // 新增评分
            Rating rating = new Rating();
            rating.setUserId(userId);
            rating.setSpotId(request.getSpotId());
            rating.setScore(request.getScore());
            rating.setComment(request.getComment());
            ratingMapper.insert(rating);
        }
        
        // 更新景点平均评分
        updateSpotAvgRating(request.getSpotId());
    }

    @Override
    public RatingResponse getUserRating(Long userId, Long spotId) {
        Rating rating = ratingMapper.selectOne(
            new LambdaQueryWrapper<Rating>()
                .eq(Rating::getUserId, userId)
                .eq(Rating::getSpotId, spotId)
                .eq(Rating::getIsDeleted, 0)
        );
        
        if (rating == null) {
            return null;
        }
        
        return convertToResponse(rating);
    }

    @Override
    public PageResult<RatingResponse> getSpotRatings(Long spotId, Integer page, Integer pageSize) {
        Page<Rating> pageObj = new Page<>(page, pageSize);
        
        pageObj = (Page<Rating>) ratingMapper.selectRatingPage(pageObj, spotId);
        
        List<RatingResponse> list = pageObj.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        return PageResult.of(list, pageObj.getTotal(), page, pageSize);
    }

    @Override
    public int getUserRatingCount(Long userId) {
        return Math.toIntExact(ratingMapper.selectCount(
            new LambdaQueryWrapper<Rating>()
                .eq(Rating::getUserId, userId)
                .eq(Rating::getIsDeleted, 0)
        ));
    }

    private void updateSpotAvgRating(Long spotId) {
        // 计算平均评分
        LambdaQueryWrapper<Rating> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Rating::getSpotId, spotId);
        wrapper.eq(Rating::getIsDeleted, 0);
        List<Rating> ratings = ratingMapper.selectList(wrapper);
        
        if (ratings.isEmpty()) {
            return;
        }
        
        double avg = ratings.stream()
                .mapToInt(Rating::getScore)
                .average()
                .orElse(0);
        
        BigDecimal avgRating = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP);
        
        // 更新景点
        Spot spot = spotMapper.selectById(spotId);
        spot.setAvgRating(avgRating);
        spot.setRatingCount(ratings.size());
        spotMapper.updateById(spot);
    }

    private RatingResponse convertToResponse(Rating rating) {
        User user = userMapper.selectById(rating.getUserId());
        
        return RatingResponse.builder()
                .id(rating.getId())
                .userId(rating.getUserId())
                .spotId(rating.getSpotId())
                .score(rating.getScore())
                .comment(rating.getComment())
                .nickname(user != null ? user.getNickname() : "匿名用户")
                .avatar(user != null ? user.getAvatar() : null)
                .createdAt(rating.getCreatedAt() != null ? rating.getCreatedAt().format(DATE_FORMATTER) : null)
                .build();
    }
}
