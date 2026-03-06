package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.PageResult;
import com.travel.common.result.ResultCode;
import com.travel.dto.spot.SpotListResponse;
import com.travel.entity.UserSpotFavorite;
import com.travel.entity.SpotRegion;
import com.travel.entity.Spot;
import com.travel.entity.SpotCategory;
import com.travel.mapper.UserSpotFavoriteMapper;
import com.travel.mapper.SpotRegionMapper;
import com.travel.mapper.SpotCategoryMapper;
import com.travel.mapper.SpotMapper;
import com.travel.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 收藏服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final UserSpotFavoriteMapper userSpotFavoriteMapper;
    private final SpotMapper spotMapper;
    private final SpotRegionMapper spotRegionMapper;
    private final SpotCategoryMapper spotCategoryMapper;

    @Override
    public void addFavorite(Long userId, Long spotId) {
        // 检查景点是否存在
        Spot spot = spotMapper.selectById(spotId);
        if (spot == null || spot.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.SPOT_NOT_FOUND);
        }
        if (spot.getIsPublished() != 1) {
            throw new BusinessException(ResultCode.SPOT_OFFLINE);
        }
        
        // 检查是否已收藏
        UserSpotFavorite existingFavorite = userSpotFavoriteMapper.selectOne(
            new LambdaQueryWrapper<UserSpotFavorite>()
                .eq(UserSpotFavorite::getUserId, userId)
                .eq(UserSpotFavorite::getSpotId, spotId)
        );
        if (existingFavorite != null) {
            if (existingFavorite.getIsDeleted() == 0) {
                return; // 幂等处理
            }
            existingFavorite.setIsDeleted(0);
            userSpotFavoriteMapper.updateById(existingFavorite);
            return;
        }
        
        UserSpotFavorite favorite = new UserSpotFavorite();
        favorite.setUserId(userId);
        favorite.setSpotId(spotId);
        userSpotFavoriteMapper.insert(favorite);
        log.info("用户添加收藏: userId={}, spotId={}", userId, spotId);
    }

    @Override
    public void removeFavorite(Long userId, Long spotId) {
        UserSpotFavorite deletedFavorite = new UserSpotFavorite();
        deletedFavorite.setIsDeleted(1);
        userSpotFavoriteMapper.update(
            deletedFavorite,
            new LambdaQueryWrapper<UserSpotFavorite>()
                .eq(UserSpotFavorite::getUserId, userId)
                .eq(UserSpotFavorite::getSpotId, spotId)
        );
        log.info("用户取消收藏: userId={}, spotId={}", userId, spotId);
    }

    @Override
    public boolean isFavorite(Long userId, Long spotId) {
        return userSpotFavoriteMapper.selectCount(
            new LambdaQueryWrapper<UserSpotFavorite>()
                .eq(UserSpotFavorite::getUserId, userId)
                .eq(UserSpotFavorite::getSpotId, spotId)
                .eq(UserSpotFavorite::getIsDeleted, 0)
        ) > 0;
    }

    @Override
    public PageResult<SpotListResponse> getFavoriteList(Long userId, Integer page, Integer pageSize) {
        // 分页查询收藏记录
        Page<UserSpotFavorite> pageObj = new Page<>(page, pageSize);
        Page<UserSpotFavorite> favoriteResult = userSpotFavoriteMapper.selectPage(pageObj,
            new LambdaQueryWrapper<UserSpotFavorite>()
                .eq(UserSpotFavorite::getUserId, userId)
                .eq(UserSpotFavorite::getIsDeleted, 0)
                .orderByDesc(UserSpotFavorite::getCreatedAt)
        );
        
        if (favoriteResult.getRecords().isEmpty()) {
            return PageResult.of(new ArrayList<>(), 0L, page, pageSize);
        }
        
        // 获取景点ID列表
        List<Long> spotIds = favoriteResult.getRecords().stream()
                .map(UserSpotFavorite::getSpotId)
                .collect(Collectors.toList());
        
        // 批量查询景点
        List<Spot> spots = spotMapper.selectBatchIds(spotIds);
        
        // 转换为响应
        List<SpotListResponse> list = spots.stream()
                .filter(spot -> spot.getIsDeleted() == 0 && spot.getIsPublished() == 1)
                .map(this::convertToListResponse)
                .collect(Collectors.toList());
        
        return PageResult.of(list, favoriteResult.getTotal(), page, pageSize);
    }

    private SpotListResponse convertToListResponse(Spot spot) {
        return SpotListResponse.builder()
                .id(spot.getId())
                .name(spot.getName())
                .coverImage(spot.getCoverImageUrl())
                .price(spot.getPrice())
                .avgRating(spot.getAvgRating())
                .ratingCount(spot.getRatingCount())
                .regionName(getRegionName(spot.getRegionId()))
                .categoryName(getCategoryName(spot.getCategoryId()))
                .build();
    }

    private String getRegionName(Long regionId) {
        if (regionId == null) return null;
        SpotRegion region = spotRegionMapper.selectById(regionId);
        return region != null && region.getIsDeleted() == 0 ? region.getName() : null;
    }

    private String getCategoryName(Long categoryId) {
        if (categoryId == null) return null;
        SpotCategory category = spotCategoryMapper.selectById(categoryId);
        return category != null && category.getIsDeleted() == 0 ? category.getName() : null;
    }
}
