package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.PageResult;
import com.travel.common.result.ResultCode;
import com.travel.dto.spot.request.SpotListRequest;
import com.travel.dto.spot.response.SpotDetailResponse;
import com.travel.dto.spot.response.SpotFilterResponse;
import com.travel.dto.spot.response.SpotListResponse;
import com.travel.dto.spot.response.SpotViewHistoryResponse;
import com.travel.entity.Review;
import com.travel.entity.Spot;
import com.travel.entity.SpotImage;
import com.travel.entity.UserSpotFavorite;
import com.travel.entity.UserSpotView;
import com.travel.mapper.ReviewMapper;
import com.travel.mapper.SpotImageMapper;
import com.travel.mapper.SpotMapper;
import com.travel.mapper.UserSpotFavoriteMapper;
import com.travel.mapper.UserSpotViewMapper;
import com.travel.service.SpotQueryService;
import com.travel.service.support.spot.SpotSupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 景点查询服务实现，负责用户端列表、详情、历史和筛选。
 */
@Service
@RequiredArgsConstructor
public class SpotQueryServiceImpl implements SpotQueryService {

    private static final DateTimeFormatter VIEW_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SpotMapper spotMapper;
    private final SpotImageMapper spotImageMapper;
    private final UserSpotFavoriteMapper userSpotFavoriteMapper;
    private final ReviewMapper reviewMapper;
    private final UserSpotViewMapper userSpotViewMapper;
    private final SpotSupportService spotSupportService;

    @Override
    public PageResult<SpotListResponse> getSpotList(SpotListRequest request) {
        Page<Spot> page = new Page<>(request.getPage(), request.getPageSize());
        LambdaQueryWrapper<Spot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Spot::getIsPublished, 1);
        wrapper.eq(Spot::getIsDeleted, 0);

        if (request.getRegionId() != null) {
            Set<Long> regionIds = spotSupportService.findRegionAndChildrenIds(request.getRegionId());
            if (regionIds.isEmpty() || regionIds.size() == 1) {
                wrapper.eq(Spot::getRegionId, request.getRegionId());
            } else {
                wrapper.in(Spot::getRegionId, regionIds);
            }
        }
        if (request.getCategoryId() != null) {
            Set<Long> categoryIds = spotSupportService.findCategoryAndChildrenIds(request.getCategoryId());
            if (categoryIds.isEmpty()) {
                wrapper.eq(Spot::getCategoryId, request.getCategoryId());
            } else {
                wrapper.in(Spot::getCategoryId, categoryIds);
            }
        }

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
            .map(spotSupportService::convertToListResponse)
            .collect(Collectors.toList());
        return PageResult.of(list, result.getTotal(), request.getPage(), request.getPageSize());
    }

    @Override
    public PageResult<SpotListResponse> searchSpots(String keyword, Integer page, Integer pageSize) {
        Page<Spot> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<Spot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Spot::getIsPublished, 1);
        wrapper.eq(Spot::getIsDeleted, 0);
        wrapper.and(query -> query.like(Spot::getName, keyword).or().like(Spot::getDescription, keyword));
        wrapper.orderByDesc(Spot::getHeatScore);

        Page<Spot> result = spotMapper.selectPage(pageObj, wrapper);
        List<SpotListResponse> list = result.getRecords().stream()
            .map(spotSupportService::convertToListResponse)
            .collect(Collectors.toList());
        return PageResult.of(list, result.getTotal(), page, pageSize);
    }

    @Override
    public PageResult<SpotViewHistoryResponse> getViewHistory(Long userId, Integer page, Integer pageSize) {
        if (userId == null) {
            return PageResult.of(new ArrayList<>(), 0L, page, pageSize);
        }

        List<UserSpotView> views = userSpotViewMapper.selectList(
            new LambdaQueryWrapper<UserSpotView>()
                .eq(UserSpotView::getUserId, userId)
                .orderByDesc(UserSpotView::getCreatedAt)
                .orderByDesc(UserSpotView::getId)
        );

        Map<Long, UserSpotView> latestBySpot = new LinkedHashMap<>();
        for (UserSpotView view : views) {
            if (view.getSpotId() == null || latestBySpot.containsKey(view.getSpotId())) {
                continue;
            }
            latestBySpot.put(view.getSpotId(), view);
        }

        List<UserSpotView> latestViews = new ArrayList<>(latestBySpot.values());
        if (latestViews.isEmpty()) {
            return PageResult.of(new ArrayList<>(), 0L, page, pageSize);
        }

        Set<Long> spotIds = latestViews.stream().map(UserSpotView::getSpotId).collect(Collectors.toSet());
        Map<Long, Spot> spotMap = spotMapper.selectBatchIds(spotIds).stream()
            .filter(spot -> spot.getIsDeleted() == 0 && spot.getIsPublished() == 1)
            .collect(Collectors.toMap(Spot::getId, spot -> spot));

        List<SpotViewHistoryResponse> allItems = latestViews.stream()
            .map(view -> {
                Spot spot = spotMap.get(view.getSpotId());
                if (spot == null) {
                    return null;
                }
                return SpotViewHistoryResponse.builder()
                    .id(spot.getId())
                    .name(spot.getName())
                    .coverImage(spot.getCoverImageUrl())
                    .regionName(spotSupportService.getRegionName(spot.getRegionId()))
                    .categoryName(spotSupportService.getCategoryName(spot.getCategoryId()))
                    .viewedAt(view.getCreatedAt() == null ? null : view.getCreatedAt().format(VIEW_TIME_FORMATTER))
                    .build();
            })
            .filter(java.util.Objects::nonNull)
            .sorted(Comparator.comparing(SpotViewHistoryResponse::getViewedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());

        long total = allItems.size();
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int fromIndex = Math.min((safePage - 1) * safePageSize, allItems.size());
        int toIndex = Math.min(fromIndex + safePageSize, allItems.size());
        return PageResult.of(new ArrayList<>(allItems.subList(fromIndex, toIndex)), total, safePage, safePageSize);
    }

    @Override
    public SpotDetailResponse getSpotDetail(Long spotId, Long userId) {
        Spot spot = spotMapper.selectById(spotId);
        if (spot == null || spot.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.SPOT_NOT_FOUND);
        }
        if (spot.getIsPublished() != 1) {
            throw new BusinessException(ResultCode.SPOT_OFFLINE);
        }

        List<SpotImage> images = spotImageMapper.selectList(
            new LambdaQueryWrapper<SpotImage>()
                .eq(SpotImage::getSpotId, spotId)
                .eq(SpotImage::getIsDeleted, 0)
                .orderByAsc(SpotImage::getSortOrder)
        );
        List<String> imageUrls = images.stream().map(SpotImage::getImageUrl).collect(Collectors.toList());
        if (StringUtils.hasText(spot.getCoverImageUrl())) {
            imageUrls.add(0, spot.getCoverImageUrl());
        }

        Boolean isFavorite = false;
        Integer userRating = null;
        if (userId != null) {
            Long favoriteCount = userSpotFavoriteMapper.selectCount(
                new LambdaQueryWrapper<UserSpotFavorite>()
                    .eq(UserSpotFavorite::getUserId, userId)
                    .eq(UserSpotFavorite::getSpotId, spotId)
                    .eq(UserSpotFavorite::getIsDeleted, 0)
            );
            isFavorite = favoriteCount > 0;

            Review review = reviewMapper.selectOne(
                new LambdaQueryWrapper<Review>()
                    .eq(Review::getUserId, userId)
                    .eq(Review::getSpotId, spotId)
                    .eq(Review::getIsDeleted, 0)
            );
            if (review != null) {
                userRating = review.getScore();
            }
        }

        List<SpotDetailResponse.CommentItem> comments = reviewMapper.selectLatestComments(spotId, 5);
        return SpotDetailResponse.builder()
            .id(spot.getId())
            .name(spot.getName())
            .coverImage(spot.getCoverImageUrl())
            .description(spot.getDescription())
            .price(spot.getPrice())
            .openTime(spot.getOpenTime())
            .address(spot.getAddress())
            .latitude(spot.getLatitude())
            .longitude(spot.getLongitude())
            .images(imageUrls)
            .avgRating(spot.getAvgRating())
            .ratingCount(spot.getRatingCount())
            .regionName(spotSupportService.getRegionName(spot.getRegionId()))
            .categoryName(spotSupportService.getCategoryName(spot.getCategoryId()))
            .isFavorite(isFavorite)
            .userRating(userRating)
            .latestComments(comments)
            .build();
    }

    @Override
    public SpotFilterResponse getFilters() {
        return spotSupportService.getFilters();
    }
}
