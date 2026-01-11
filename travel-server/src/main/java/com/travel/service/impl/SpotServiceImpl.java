package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.PageResult;
import com.travel.common.result.ResultCode;
import com.travel.dto.spot.*;
import com.travel.entity.*;
import com.travel.mapper.*;
import com.travel.service.SpotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 景点服务实现
 */
@Service
@RequiredArgsConstructor
public class SpotServiceImpl implements SpotService {

    private final SpotMapper spotMapper;
    private final SpotImageMapper spotImageMapper;
    private final RegionMapper regionMapper;
    private final SpotCategoryMapper spotCategoryMapper;
    private final FavoriteMapper favoriteMapper;
    private final RatingMapper ratingMapper;

    @Override
    public PageResult<SpotListResponse> getSpotList(SpotListRequest request) {
        Page<Spot> page = new Page<>(request.getPage(), request.getPageSize());
        
        LambdaQueryWrapper<Spot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Spot::getPublished, 1);
        
        if (request.getRegionId() != null) {
            wrapper.eq(Spot::getRegionId, request.getRegionId());
        }
        if (request.getCategoryId() != null) {
            wrapper.eq(Spot::getCategoryId, request.getCategoryId());
        }
        
        // 排序
        String sortBy = request.getSortBy();
        if ("rating".equals(sortBy)) {
            wrapper.orderByDesc(Spot::getAvgRating);
        } else if ("price_asc".equals(sortBy)) {
            wrapper.orderByAsc(Spot::getPrice);
        } else if ("price_desc".equals(sortBy)) {
            wrapper.orderByDesc(Spot::getPrice);
        } else {
            wrapper.orderByDesc(Spot::getHeatScore);
        }
        
        Page<Spot> result = spotMapper.selectPage(page, wrapper);
        
        List<SpotListResponse> list = result.getRecords().stream()
                .map(this::convertToListResponse)
                .collect(Collectors.toList());
        
        return PageResult.of(list, result.getTotal(), request.getPage(), request.getPageSize());
    }

    @Override
    public PageResult<SpotListResponse> searchSpots(String keyword, Integer page, Integer pageSize) {
        Page<Spot> pageObj = new Page<>(page, pageSize);
        
        LambdaQueryWrapper<Spot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Spot::getPublished, 1);
        wrapper.and(w -> w.like(Spot::getName, keyword).or().like(Spot::getDescription, keyword));
        wrapper.orderByDesc(Spot::getHeatScore);
        
        Page<Spot> result = spotMapper.selectPage(pageObj, wrapper);
        
        List<SpotListResponse> list = result.getRecords().stream()
                .map(this::convertToListResponse)
                .collect(Collectors.toList());
        
        return PageResult.of(list, result.getTotal(), page, pageSize);
    }

    @Override
    public SpotDetailResponse getSpotDetail(Long spotId, Long userId) {
        Spot spot = spotMapper.selectById(spotId);
        if (spot == null) {
            throw new BusinessException(ResultCode.SPOT_NOT_FOUND);
        }
        if (spot.getPublished() != 1) {
            throw new BusinessException(ResultCode.SPOT_OFFLINE);
        }
        
        // 增加热度
        spot.setHeatScore(spot.getHeatScore() + 1);
        spotMapper.updateById(spot);
        
        // 获取图片
        List<SpotImage> images = spotImageMapper.selectList(
            new LambdaQueryWrapper<SpotImage>().eq(SpotImage::getSpotId, spotId).orderByAsc(SpotImage::getSortOrder)
        );
        List<String> imageUrls = images.stream().map(SpotImage::getImageUrl).collect(Collectors.toList());
        if (StringUtils.hasText(spot.getCoverImage())) {
            imageUrls.add(0, spot.getCoverImage());
        }
        
        // 获取地区和分类名称
        String regionName = getRegionName(spot.getRegionId());
        String categoryName = getCategoryName(spot.getCategoryId());
        
        // 检查收藏状态
        Boolean isFavorite = false;
        Integer userRating = null;
        if (userId != null) {
            Long favoriteCount = favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getUserId, userId)
                    .eq(Favorite::getSpotId, spotId)
            );
            isFavorite = favoriteCount > 0;
            
            Rating rating = ratingMapper.selectOne(
                new LambdaQueryWrapper<Rating>()
                    .eq(Rating::getUserId, userId)
                    .eq(Rating::getSpotId, spotId)
            );
            if (rating != null) {
                userRating = rating.getScore();
            }
        }
        
        // 获取最新评论
        List<SpotDetailResponse.CommentItem> comments = ratingMapper.selectLatestComments(spotId, 5);
        
        return SpotDetailResponse.builder()
                .id(spot.getId())
                .name(spot.getName())
                .description(spot.getDescription())
                .price(spot.getPrice())
                .openTime(spot.getOpenTime())
                .address(spot.getAddress())
                .latitude(spot.getLatitude())
                .longitude(spot.getLongitude())
                .images(imageUrls)
                .avgRating(spot.getAvgRating())
                .ratingCount(spot.getRatingCount())
                .regionName(regionName)
                .categoryName(categoryName)
                .isFavorite(isFavorite)
                .userRating(userRating)
                .latestComments(comments)
                .build();
    }

    @Override
    public SpotFilterResponse getFilters() {
        List<Region> regions = regionMapper.selectList(null);
        List<SpotCategory> categories = spotCategoryMapper.selectList(null);
        
        return SpotFilterResponse.builder()
                .regions(regions.stream()
                        .map(r -> SpotFilterResponse.FilterItem.builder().id(r.getId()).name(r.getName()).build())
                        .collect(Collectors.toList()))
                .categories(categories.stream()
                        .map(c -> SpotFilterResponse.FilterItem.builder().id(c.getId()).name(c.getName()).build())
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    public PageResult<AdminSpotListResponse> getAdminSpotList(AdminSpotListRequest request) {
        Page<Spot> page = new Page<>(request.getPage(), request.getPageSize());
        
        LambdaQueryWrapper<Spot> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.like(Spot::getName, request.getKeyword());
        }
        if (request.getRegionId() != null) {
            wrapper.eq(Spot::getRegionId, request.getRegionId());
        }
        if (request.getCategoryId() != null) {
            wrapper.eq(Spot::getCategoryId, request.getCategoryId());
        }
        if (request.getPublished() != null) {
            wrapper.eq(Spot::getPublished, request.getPublished() == 1);
        }
        wrapper.orderByDesc(Spot::getCreatedAt);
        
        Page<Spot> result = spotMapper.selectPage(page, wrapper);
        
        List<AdminSpotListResponse> list = result.getRecords().stream()
                .map(this::convertToAdminListResponse)
                .collect(Collectors.toList());
        
        return PageResult.of(list, result.getTotal(), request.getPage(), request.getPageSize());
    }

    @Override
    public AdminSpotRequest getAdminSpotDetail(Long spotId) {
        Spot spot = spotMapper.selectById(spotId);
        if (spot == null) {
            throw new BusinessException(ResultCode.SPOT_NOT_FOUND);
        }
        
        List<SpotImage> images = spotImageMapper.selectList(
            new LambdaQueryWrapper<SpotImage>().eq(SpotImage::getSpotId, spotId)
        );
        
        AdminSpotRequest response = new AdminSpotRequest();
        response.setName(spot.getName());
        response.setDescription(spot.getDescription());
        response.setPrice(spot.getPrice());
        response.setOpenTime(spot.getOpenTime());
        response.setAddress(spot.getAddress());
        response.setLatitude(spot.getLatitude());
        response.setLongitude(spot.getLongitude());
        response.setCoverImage(spot.getCoverImage());
        response.setImages(images.stream().map(SpotImage::getImageUrl).collect(Collectors.toList()));
        response.setRegionId(spot.getRegionId());
        response.setCategoryId(spot.getCategoryId());
        response.setPublished(spot.getPublished() == 1);
        
        return response;
    }

    @Override
    @Transactional
    public Long createSpot(AdminSpotRequest request) {
        Spot spot = new Spot();
        copyProperties(request, spot);
        spotMapper.insert(spot);
        
        // 保存图片
        saveSpotImages(spot.getId(), request.getImages());
        
        return spot.getId();
    }

    @Override
    @Transactional
    public void updateSpot(Long spotId, AdminSpotRequest request) {
        Spot spot = spotMapper.selectById(spotId);
        if (spot == null) {
            throw new BusinessException(ResultCode.SPOT_NOT_FOUND);
        }
        
        copyProperties(request, spot);
        spotMapper.updateById(spot);
        
        // 更新图片
        spotImageMapper.delete(new LambdaQueryWrapper<SpotImage>().eq(SpotImage::getSpotId, spotId));
        saveSpotImages(spotId, request.getImages());
    }

    @Override
    public void updatePublishStatus(Long spotId, Boolean published) {
        Spot spot = spotMapper.selectById(spotId);
        if (spot == null) {
            throw new BusinessException(ResultCode.SPOT_NOT_FOUND);
        }
        spot.setPublished(published ? 1 : 0);
        spotMapper.updateById(spot);
    }

    @Override
    public void deleteSpot(Long spotId) {
        spotMapper.deleteById(spotId);
        spotImageMapper.delete(new LambdaQueryWrapper<SpotImage>().eq(SpotImage::getSpotId, spotId));
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

    private AdminSpotListResponse convertToAdminListResponse(Spot spot) {
        return AdminSpotListResponse.builder()
                .id(spot.getId())
                .name(spot.getName())
                .coverImage(spot.getCoverImage())
                .price(spot.getPrice())
                .regionName(getRegionName(spot.getRegionId()))
                .categoryName(getCategoryName(spot.getCategoryId()))
                .avgRating(spot.getAvgRating())
                .heatScore(spot.getHeatScore())
                .published(spot.getPublished() == 1)
                .createdAt(spot.getCreatedAt())
                .build();
    }

    private String getRegionName(Long regionId) {
        if (regionId == null) return null;
        Region region = regionMapper.selectById(regionId);
        return region != null ? region.getName() : null;
    }

    private String getCategoryName(Long categoryId) {
        if (categoryId == null) return null;
        SpotCategory category = spotCategoryMapper.selectById(categoryId);
        return category != null ? category.getName() : null;
    }

    private void copyProperties(AdminSpotRequest request, Spot spot) {
        spot.setName(request.getName());
        spot.setDescription(request.getDescription());
        spot.setPrice(request.getPrice());
        spot.setOpenTime(request.getOpenTime());
        spot.setAddress(request.getAddress());
        spot.setLatitude(request.getLatitude());
        spot.setLongitude(request.getLongitude());
        spot.setCoverImage(request.getCoverImage());
        spot.setRegionId(request.getRegionId());
        spot.setCategoryId(request.getCategoryId());
        spot.setPublished(Boolean.TRUE.equals(request.getPublished()) ? 1 : 0);
    }

    private void saveSpotImages(Long spotId, List<String> images) {
        if (images == null || images.isEmpty()) return;
        for (int i = 0; i < images.size(); i++) {
            SpotImage image = new SpotImage();
            image.setSpotId(spotId);
            image.setImageUrl(images.get(i));
            image.setSortOrder(i);
            spotImageMapper.insert(image);
        }
    }
}
