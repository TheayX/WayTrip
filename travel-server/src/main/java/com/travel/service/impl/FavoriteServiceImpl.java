package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.PageResult;
import com.travel.common.result.ResultCode;
import com.travel.dto.spot.SpotListResponse;
import com.travel.entity.Favorite;
import com.travel.entity.Region;
import com.travel.entity.Spot;
import com.travel.entity.SpotCategory;
import com.travel.mapper.FavoriteMapper;
import com.travel.mapper.RegionMapper;
import com.travel.mapper.SpotCategoryMapper;
import com.travel.mapper.SpotMapper;
import com.travel.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 收藏服务实现
 */
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final SpotMapper spotMapper;
    private final RegionMapper regionMapper;
    private final SpotCategoryMapper spotCategoryMapper;

    @Override
    public void addFavorite(Long userId, Long spotId) {
        // 检查景点是否存在
        Spot spot = spotMapper.selectById(spotId);
        if (spot == null || spot.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.SPOT_NOT_FOUND);
        }
        if (spot.getPublished() != 1) {
            throw new BusinessException(ResultCode.SPOT_OFFLINE);
        }
        
        // 检查是否已收藏
        Favorite existingFavorite = favoriteMapper.selectOne(
            new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getSpotId, spotId)
        );
        if (existingFavorite != null) {
            if (existingFavorite.getIsDeleted() == 0) {
                return; // 幂等处理
            }
            existingFavorite.setIsDeleted(0);
            favoriteMapper.updateById(existingFavorite);
            return;
        }
        
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setSpotId(spotId);
        favoriteMapper.insert(favorite);
    }

    @Override
    public void removeFavorite(Long userId, Long spotId) {
        Favorite deletedFavorite = new Favorite();
        deletedFavorite.setIsDeleted(1);
        favoriteMapper.update(
            deletedFavorite,
            new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getSpotId, spotId)
        );
    }

    @Override
    public boolean isFavorite(Long userId, Long spotId) {
        return favoriteMapper.selectCount(
            new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getSpotId, spotId)
                .eq(Favorite::getIsDeleted, 0)
        ) > 0;
    }

    @Override
    public PageResult<SpotListResponse> getFavoriteList(Long userId, Integer page, Integer pageSize) {
        // 分页查询收藏记录
        Page<Favorite> pageObj = new Page<>(page, pageSize);
        Page<Favorite> favoriteResult = favoriteMapper.selectPage(pageObj,
            new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getIsDeleted, 0)
                .orderByDesc(Favorite::getCreatedAt)
        );
        
        if (favoriteResult.getRecords().isEmpty()) {
            return PageResult.of(new ArrayList<>(), 0L, page, pageSize);
        }
        
        // 获取景点ID列表
        List<Long> spotIds = favoriteResult.getRecords().stream()
                .map(Favorite::getSpotId)
                .collect(Collectors.toList());
        
        // 批量查询景点
        List<Spot> spots = spotMapper.selectBatchIds(spotIds);
        
        // 转换为响应
        List<SpotListResponse> list = spots.stream()
                .filter(spot -> spot.getIsDeleted() == 0 && spot.getPublished() == 1)
                .map(this::convertToListResponse)
                .collect(Collectors.toList());
        
        return PageResult.of(list, favoriteResult.getTotal(), page, pageSize);
    }

    private SpotListResponse convertToListResponse(Spot spot) {
        return SpotListResponse.builder()
                .id(spot.getId())
                .name(spot.getName())
                .coverImage(spot.getCoverImage())
                .price(spot.getPrice())
                .avgRating(spot.getAvgRating())
                .ratingCount(spot.getRatingCount())
                .regionName(getRegionName(spot.getRegionId()))
                .categoryName(getCategoryName(spot.getCategoryId()))
                .build();
    }

    private String getRegionName(Long regionId) {
        if (regionId == null) return null;
        Region region = regionMapper.selectById(regionId);
        return region != null && region.getIsDeleted() == 0 ? region.getName() : null;
    }

    private String getCategoryName(Long categoryId) {
        if (categoryId == null) return null;
        SpotCategory category = spotCategoryMapper.selectById(categoryId);
        return category != null && category.getIsDeleted() == 0 ? category.getName() : null;
    }
}
