package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.constant.ResourceDisplayText;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.PageResult;
import com.travel.common.result.ResultCode;
import com.travel.dto.review.stats.SpotRatingStats;
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
import com.travel.service.support.spot.SpotResponseAssembler;
import com.travel.service.support.spot.SpotTreeSupport;
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
    private final SpotResponseAssembler spotResponseAssembler;
    private final SpotTreeSupport spotTreeSupport;

    @Override
    public PageResult<SpotListResponse> getSpotList(SpotListRequest request) {
        Page<Spot> page = new Page<>(request.getPage(), request.getPageSize());
        LambdaQueryWrapper<Spot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Spot::getIsPublished, 1);
        wrapper.eq(Spot::getIsDeleted, 0);

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
            .map(spotResponseAssembler::toSpotListResponse)
            .collect(Collectors.toList());
        return PageResult.of(list, result.getTotal(), request.getPage(), request.getPageSize());
    }

    @Override
    public PageResult<SpotListResponse> searchSpots(String keyword, Integer page, Integer pageSize) {
        Page<Spot> pageObj = new Page<>(page, pageSize);
        Page<Spot> result = (Page<Spot>) spotMapper.selectPublishedSearchPage(pageObj, keyword);
        List<SpotListResponse> list = result.getRecords().stream()
            .map(spotResponseAssembler::toSpotListResponse)
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
                    .regionName(spotResponseAssembler.getRegionName(spot.getRegionId()))
                    .categoryName(spotResponseAssembler.getCategoryName(spot.getCategoryId()))
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
        List<String> imageUrls = buildSpotImageUrls(spot, images);
        UserInteractionState interactionState = loadUserInteractionState(userId, spotId);
        SpotRatingStats ratingStats = reviewMapper.selectSpotRatingStats(spotId);

        List<SpotDetailResponse.CommentItem> comments = reviewMapper.selectLatestComments(spotId, 5).stream()
            // 评价主体保留，但账号已失效时统一降级成注销文案，避免页面出现空昵称。
            .map(this::normalizeCommentAuthor)
            .collect(Collectors.toList());
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
            .regionName(spotResponseAssembler.getRegionName(spot.getRegionId()))
            .categoryName(spotResponseAssembler.getCategoryName(spot.getCategoryId()))
            .reviewCount(ratingStats == null ? 0L : ratingStats.getRatingCount())
            .favoriteCount(userSpotFavoriteMapper.selectCount(
                new LambdaQueryWrapper<UserSpotFavorite>()
                    .eq(UserSpotFavorite::getSpotId, spotId)
                    .eq(UserSpotFavorite::getIsDeleted, 0)
            ))
            .viewCount(userSpotViewMapper.selectCount(
                new LambdaQueryWrapper<UserSpotView>().eq(UserSpotView::getSpotId, spotId)
            ))
            .createdAt(spot.getCreatedAt())
            .updatedAt(spot.getUpdatedAt())
            .isFavorite(interactionState.isFavorite())
            .userRating(interactionState.userRating())
            .latestComments(comments)
            .build();
    }

    @Override
    public SpotFilterResponse getFilters() {
        return spotTreeSupport.getFilters();
    }

    /**
     * 详情页图片优先保留封面在首位，避免后台上传顺序影响首屏展示。
     */
    private List<String> buildSpotImageUrls(Spot spot, List<SpotImage> images) {
        List<String> imageUrls = images.stream().map(SpotImage::getImageUrl).collect(Collectors.toList());
        if (StringUtils.hasText(spot.getCoverImageUrl())) {
            imageUrls.add(0, spot.getCoverImageUrl());
        }
        return imageUrls;
    }

    /**
     * 聚合当前用户对景点的收藏和评分状态，减少详情方法里的查询噪音。
     */
    private UserInteractionState loadUserInteractionState(Long userId, Long spotId) {
        if (userId == null) {
            return new UserInteractionState(false, null);
        }

        Long favoriteCount = userSpotFavoriteMapper.selectCount(
            new LambdaQueryWrapper<UserSpotFavorite>()
                .eq(UserSpotFavorite::getUserId, userId)
                .eq(UserSpotFavorite::getSpotId, spotId)
                .eq(UserSpotFavorite::getIsDeleted, 0)
        );

        Review review = reviewMapper.selectOne(
            new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getSpotId, spotId)
                .eq(Review::getIsDeleted, 0)
        );

        return new UserInteractionState(favoriteCount > 0, review == null ? null : review.getScore());
    }

    private record UserInteractionState(boolean isFavorite, Integer userRating) {
    }

    /**
     * 详情页最新评论允许展示已删除账号的历史评价，但需要隐藏原始身份信息。
     */
    private SpotDetailResponse.CommentItem normalizeCommentAuthor(SpotDetailResponse.CommentItem comment) {
        if (comment == null) {
            return null;
        }
        if (StringUtils.hasText(comment.getNickname())) {
            return comment;
        }
        comment.setNickname(ResourceDisplayText.User.DEACTIVATED);
        comment.setAvatar(null);
        return comment;
    }
}
