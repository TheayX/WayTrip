package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.PageResult;
import com.travel.common.result.ResultCode;
import com.travel.dto.spot.request.AdminSpotListRequest;
import com.travel.dto.spot.request.AdminSpotUpsertRequest;
import com.travel.dto.spot.response.AdminSpotDetailResponse;
import com.travel.dto.spot.response.AdminSpotListResponse;
import com.travel.dto.review.stats.SpotRatingStats;
import com.travel.entity.Spot;
import com.travel.entity.SpotImage;
import com.travel.entity.UserSpotFavorite;
import com.travel.entity.UserSpotView;
import com.travel.mapper.SpotImageMapper;
import com.travel.mapper.SpotMapper;
import com.travel.mapper.ReviewMapper;
import com.travel.mapper.UserSpotFavoriteMapper;
import com.travel.mapper.UserSpotViewMapper;
import com.travel.service.RecommendationService;
import com.travel.service.SpotAdminService;
import com.travel.service.support.spot.SpotResponseAssembler;
import com.travel.service.support.spot.SpotTreeSupport;
import com.travel.service.support.spot.SpotWriteSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 景点管理服务实现，负责后台列表、详情和维护操作。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpotAdminServiceImpl implements SpotAdminService {

    private final SpotMapper spotMapper;
    private final SpotImageMapper spotImageMapper;
    private final ReviewMapper reviewMapper;
    private final UserSpotFavoriteMapper userSpotFavoriteMapper;
    private final UserSpotViewMapper userSpotViewMapper;
    private final SpotResponseAssembler spotResponseAssembler;
    private final SpotTreeSupport spotTreeSupport;
    private final SpotWriteSupport spotWriteSupport;
    private final RecommendationService recommendationService;

    @Override
    public PageResult<AdminSpotListResponse> getAdminSpotList(AdminSpotListRequest request) {
        Page<Spot> page = new Page<>(request.getPage(), request.getPageSize());
        LambdaQueryWrapper<Spot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Spot::getIsDeleted, 0);

        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.like(Spot::getName, request.getKeyword());
        }
        if (request.getRegionId() != null) {
            Set<Long> regionIds = spotTreeSupport.findRegionAndChildrenIds(request.getRegionId());
            if (regionIds.isEmpty() || regionIds.size() == 1) {
                wrapper.eq(Spot::getRegionId, request.getRegionId());
            } else {
                wrapper.in(Spot::getRegionId, regionIds);
            }
        }
        if (request.getCategoryId() != null) {
            Set<Long> categoryIds = spotTreeSupport.findCategoryAndChildrenIds(request.getCategoryId());
            if (categoryIds.isEmpty()) {
                wrapper.eq(Spot::getCategoryId, request.getCategoryId());
            } else {
                wrapper.in(Spot::getCategoryId, categoryIds);
            }
        }
        if (request.getPublished() != null) {
            wrapper.eq(Spot::getIsPublished, request.getPublished());
        }
        wrapper.orderByAsc(Spot::getId);

        Page<Spot> result = spotMapper.selectPage(page, wrapper);
        List<AdminSpotListResponse> list = result.getRecords().stream()
            .map(spotResponseAssembler::toAdminSpotListResponse)
            .collect(Collectors.toList());
        return PageResult.of(list, result.getTotal(), request.getPage(), request.getPageSize());
    }

    @Override
    public AdminSpotDetailResponse getAdminSpotDetail(Long spotId) {
        Spot spot = getExistingSpot(spotId);

        List<SpotImage> images = spotImageMapper.selectList(
            new LambdaQueryWrapper<SpotImage>()
                .eq(SpotImage::getSpotId, spotId)
                .eq(SpotImage::getIsDeleted, 0)
        );

        AdminSpotDetailResponse response = new AdminSpotDetailResponse();
        response.setName(spot.getName());
        response.setDescription(spot.getDescription());
        response.setPrice(spot.getPrice());
        response.setOpenTime(spot.getOpenTime());
        response.setAddress(spot.getAddress());
        response.setLatitude(spot.getLatitude());
        response.setLongitude(spot.getLongitude());
        response.setCoverImage(spot.getCoverImageUrl());
        response.setImages(images.stream().map(SpotImage::getImageUrl).collect(Collectors.toList()));
        response.setRegionId(spot.getRegionId());
        response.setRegionName(spotResponseAssembler.getRegionName(spot.getRegionId()));
        response.setCategoryId(spot.getCategoryId());
        response.setCategoryName(spotResponseAssembler.getCategoryName(spot.getCategoryId()));
        SpotRatingStats ratingStats = reviewMapper.selectSpotRatingStats(spotId);
        response.setReviewCount(ratingStats == null ? 0L : ratingStats.getRatingCount());
        response.setFavoriteCount(userSpotFavoriteMapper.selectCount(
            new LambdaQueryWrapper<UserSpotFavorite>()
                .eq(UserSpotFavorite::getSpotId, spotId)
                .eq(UserSpotFavorite::getIsDeleted, 0)
        ));
        response.setViewCount(userSpotViewMapper.selectCount(
            new LambdaQueryWrapper<UserSpotView>().eq(UserSpotView::getSpotId, spotId)
        ));
        response.setPublished(spot.getIsPublished() == 1);
        response.setAvgRating(spot.getAvgRating());
        response.setRatingCount(spot.getRatingCount());
        response.setHeatLevel(spot.getHeatLevel());
        response.setHeatScore(spot.getHeatScore());
        response.setCreatedAt(spot.getCreatedAt());
        response.setUpdatedAt(spot.getUpdatedAt());
        return response;
    }

    @Override
    @Transactional
    public Long createSpot(AdminSpotUpsertRequest request) {
        Spot spot = new Spot();
        spotWriteSupport.copyUpsertRequest(request, spot);
        spotMapper.insert(spot);
        spotWriteSupport.saveSpotImages(spot.getId(), request.getImages());
        recommendationService.invalidateGlobalRecommendationCaches();
        log.info("景点创建成功: spotId={}, name={}", spot.getId(), spot.getName());
        return spot.getId();
    }

    @Override
    @Transactional
    public void updateSpot(Long spotId, AdminSpotUpsertRequest request) {
        Spot spot = getExistingSpot(spotId);
        spotWriteSupport.copyUpsertRequest(request, spot);
        spotMapper.updateById(spot);

        if (request.getImages() != null) {
            markSpotImagesDeleted(spotId);
            spotWriteSupport.saveSpotImages(spotId, request.getImages());
        }

        recommendationService.invalidateGlobalRecommendationCaches();

        log.info("景点更新成功: spotId={}, name={}", spotId, request.getName());
    }

    @Override
    public void updatePublishStatus(Long spotId, Boolean published) {
        Spot spot = getExistingSpot(spotId);
        spot.setIsPublished(Boolean.TRUE.equals(published) ? 1 : 0);
        spotMapper.updateById(spot);
        recommendationService.invalidateGlobalRecommendationCaches();
        log.info("景点发布状态变更: spotId={}, published={}", spotId, published);
    }

    @Override
    public void deleteSpot(Long spotId) {
        Spot spot = getExistingSpot(spotId);
        spot.setIsDeleted(1);
        spotMapper.updateById(spot);

        markSpotImagesDeleted(spotId);
        recommendationService.invalidateGlobalRecommendationCaches();
        log.info("景点已删除: spotId={}, name={}", spotId, spot.getName());
    }

    private Spot getExistingSpot(Long spotId) {
        Spot spot = spotMapper.selectById(spotId);
        if (spot == null || spot.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.SPOT_NOT_FOUND);
        }
        return spot;
    }

    /**
     * 更新图片时统一先软删旧记录，避免历史图片残留在当前景点下继续生效。
     */
    private void markSpotImagesDeleted(Long spotId) {
        SpotImage deletedImage = new SpotImage();
        deletedImage.setIsDeleted(1);
        spotImageMapper.update(
            deletedImage,
            new LambdaQueryWrapper<SpotImage>().eq(SpotImage::getSpotId, spotId)
        );
    }
}
