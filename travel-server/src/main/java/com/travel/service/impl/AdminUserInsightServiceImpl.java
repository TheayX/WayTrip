package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.PageResult;
import com.travel.common.result.ResultCode;
import com.travel.dto.user.model.AdminUserFavoriteListItem;
import com.travel.dto.user.model.AdminUserPreferenceListItem;
import com.travel.dto.user.model.AdminUserViewListItem;
import com.travel.dto.user.request.AdminUserFavoriteListRequest;
import com.travel.dto.user.request.AdminUserPreferenceListRequest;
import com.travel.dto.user.request.AdminUserViewListRequest;
import com.travel.entity.*;
import com.travel.mapper.*;
import com.travel.service.AdminUserInsightService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理端用户行为与画像查询服务。
 */
@Service
@RequiredArgsConstructor
public class AdminUserInsightServiceImpl implements AdminUserInsightService {

    // 时间格式配置
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // 持久层依赖
    private final UserMapper userMapper;
    private final SpotMapper spotMapper;
    private final SpotCategoryMapper spotCategoryMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final UserSpotFavoriteMapper userSpotFavoriteMapper;
    private final UserSpotViewMapper userSpotViewMapper;

    // 用户偏好查询
    @Override
    public PageResult<AdminUserPreferenceListItem> getPreferenceList(AdminUserPreferenceListRequest request) {
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();

        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            userWrapper.like(User::getNickname, request.getNickname().trim());
        }

        if (request.getCategoryId() != null) {
            Set<Long> matchedUserIds = userPreferenceMapper.selectList(
                new LambdaQueryWrapper<UserPreference>()
                    .eq(UserPreference::getIsDeleted, 0)
                    .eq(UserPreference::getTag, String.valueOf(request.getCategoryId()))
                    .select(UserPreference::getUserId)
            ).stream().map(UserPreference::getUserId).collect(Collectors.toSet());

            if (matchedUserIds.isEmpty()) {
                return emptyPageResult(request.getPage(), request.getPageSize());
            }
            userWrapper.in(User::getId, matchedUserIds);
        }

        userWrapper.orderByDesc(User::getUpdatedAt);

        Page<User> page = new Page<>(request.getPage(), request.getPageSize());
        Page<User> result = userMapper.selectPage(page, userWrapper);
        List<User> users = result.getRecords();
        if (users.isEmpty()) {
            return PageResult.of(Collections.emptyList(), result.getTotal(), request.getPage(), request.getPageSize());
        }

        Set<Long> userIds = users.stream().map(User::getId).collect(Collectors.toSet());
        List<UserPreference> preferences = userPreferenceMapper.selectList(
            new LambdaQueryWrapper<UserPreference>()
                .in(UserPreference::getUserId, userIds)
                .eq(UserPreference::getIsDeleted, 0)
        );

        Map<Long, List<UserPreference>> preferenceMap = preferences.stream()
            .collect(Collectors.groupingBy(UserPreference::getUserId));

        Set<Long> categoryIds = preferences.stream()
            .map(UserPreference::getTag)
            .map(this::parseCategoryId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Map<Long, String> categoryNameMap = spotCategoryMapper.selectBatchIds(categoryIds).stream()
            .collect(Collectors.toMap(SpotCategory::getId, SpotCategory::getName));

        List<AdminUserPreferenceListItem> list = users.stream().map(user -> {
            List<UserPreference> userPreferences = preferenceMap.getOrDefault(user.getId(), Collections.emptyList());
            String latestUpdatedAt = userPreferences.stream()
                .map(UserPreference::getUpdatedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .map(DATE_TIME_FORMATTER::format)
                .orElse(null);

            List<String> tags = userPreferences.stream()
                .map(UserPreference::getTag)
                .map(this::parseCategoryId)
                .filter(Objects::nonNull)
                .map(categoryNameMap::get)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

            return AdminUserPreferenceListItem.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .preferenceTags(tags)
                .updatedAt(latestUpdatedAt)
                .createdAt(formatDateTime(user.getCreatedAt()))
                .build();
        }).collect(Collectors.toList());

        return PageResult.of(list, result.getTotal(), request.getPage(), request.getPageSize());
    }

    // 用户收藏查询与治理
    @Override
    public PageResult<AdminUserFavoriteListItem> getFavoriteList(AdminUserFavoriteListRequest request) {
        LambdaQueryWrapper<UserSpotFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSpotFavorite::getIsDeleted, 0);

        Set<Long> userIds = resolveUserIdsByNickname(request.getNickname());
        if (userIds != null) {
            if (userIds.isEmpty()) {
                return emptyPageResult(request.getPage(), request.getPageSize());
            }
            wrapper.in(UserSpotFavorite::getUserId, userIds);
        }

        Set<Long> spotIds = resolveSpotIdsByName(request.getSpotName());
        if (spotIds != null) {
            if (spotIds.isEmpty()) {
                return emptyPageResult(request.getPage(), request.getPageSize());
            }
            wrapper.in(UserSpotFavorite::getSpotId, spotIds);
        }

        if (request.getStartDate() != null) {
            wrapper.ge(UserSpotFavorite::getCreatedAt, request.getStartDate().atStartOfDay());
        }
        if (request.getEndDate() != null) {
            wrapper.le(UserSpotFavorite::getCreatedAt, request.getEndDate().atTime(23, 59, 59));
        }

        wrapper.orderByDesc(UserSpotFavorite::getCreatedAt);

        Page<UserSpotFavorite> page = new Page<>(request.getPage(), request.getPageSize());
        Page<UserSpotFavorite> result = userSpotFavoriteMapper.selectPage(page, wrapper);
        List<UserSpotFavorite> favorites = result.getRecords();

        List<AdminUserFavoriteListItem> list = buildFavoriteItems(favorites);
        return PageResult.of(list, result.getTotal(), request.getPage(), request.getPageSize());
    }

    @Override
    @Transactional
    public void deleteFavorite(Long favoriteId) {
        UserSpotFavorite favorite = userSpotFavoriteMapper.selectById(favoriteId);
        if (favorite == null || favorite.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "收藏记录不存在");
        }

        favorite.setIsDeleted(1);
        userSpotFavoriteMapper.updateById(favorite);
    }

    // 用户浏览查询与治理
    @Override
    public PageResult<AdminUserViewListItem> getViewList(AdminUserViewListRequest request) {
        LambdaQueryWrapper<UserSpotView> wrapper = new LambdaQueryWrapper<>();

        Set<Long> userIds = resolveUserIdsByNickname(request.getNickname());
        if (userIds != null) {
            if (userIds.isEmpty()) {
                return emptyPageResult(request.getPage(), request.getPageSize());
            }
            wrapper.in(UserSpotView::getUserId, userIds);
        }

        Set<Long> spotIds = resolveSpotIdsByName(request.getSpotName());
        if (spotIds != null) {
            if (spotIds.isEmpty()) {
                return emptyPageResult(request.getPage(), request.getPageSize());
            }
            wrapper.in(UserSpotView::getSpotId, spotIds);
        }

        if (request.getSource() != null && !request.getSource().isBlank()) {
            wrapper.eq(UserSpotView::getViewSource, request.getSource().trim());
        }
        if (request.getStartDate() != null) {
            wrapper.ge(UserSpotView::getCreatedAt, request.getStartDate().atStartOfDay());
        }
        if (request.getEndDate() != null) {
            wrapper.le(UserSpotView::getCreatedAt, request.getEndDate().atTime(23, 59, 59));
        }

        wrapper.orderByDesc(UserSpotView::getCreatedAt).orderByDesc(UserSpotView::getId);

        Page<UserSpotView> page = new Page<>(request.getPage(), request.getPageSize());
        Page<UserSpotView> result = userSpotViewMapper.selectPage(page, wrapper);
        List<UserSpotView> views = result.getRecords();

        List<AdminUserViewListItem> list = buildViewItems(views);
        return PageResult.of(list, result.getTotal(), request.getPage(), request.getPageSize());
    }

    @Override
    @Transactional
    public void deleteView(Long viewId) {
        UserSpotView view = userSpotViewMapper.selectById(viewId);
        if (view == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "浏览记录不存在");
        }
        userSpotViewMapper.deleteById(viewId);
    }

    // 列表项转换方法
    private List<AdminUserFavoriteListItem> buildFavoriteItems(List<UserSpotFavorite> favorites) {
        if (favorites.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, User> userMap = userMapper.selectBatchIds(
            favorites.stream().map(UserSpotFavorite::getUserId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(User::getId, Function.identity()));

        Map<Long, Spot> spotMap = spotMapper.selectBatchIds(
            favorites.stream().map(UserSpotFavorite::getSpotId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Spot::getId, Function.identity()));

        return favorites.stream().map(favorite -> {
            User user = userMap.get(favorite.getUserId());
            Spot spot = spotMap.get(favorite.getSpotId());
            return AdminUserFavoriteListItem.builder()
                .id(favorite.getId())
                .userId(favorite.getUserId())
                .nickname(user != null ? user.getNickname() : "未知用户")
                .spotId(favorite.getSpotId())
                .spotName(spot != null ? spot.getName() : "景点#" + favorite.getSpotId())
                .coverImage(spot != null ? spot.getCoverImageUrl() : null)
                .createdAt(formatDateTime(favorite.getCreatedAt()))
                .build();
        }).collect(Collectors.toList());
    }

    private List<AdminUserViewListItem> buildViewItems(List<UserSpotView> views) {
        if (views.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, User> userMap = userMapper.selectBatchIds(
            views.stream().map(UserSpotView::getUserId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(User::getId, Function.identity()));

        Map<Long, Spot> spotMap = spotMapper.selectBatchIds(
            views.stream().map(UserSpotView::getSpotId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Spot::getId, Function.identity()));

        return views.stream().map(view -> {
            User user = userMap.get(view.getUserId());
            Spot spot = spotMap.get(view.getSpotId());
            return AdminUserViewListItem.builder()
                .id(view.getId())
                .userId(view.getUserId())
                .nickname(user != null ? user.getNickname() : "未知用户")
                .spotId(view.getSpotId())
                .spotName(spot != null ? spot.getName() : "景点#" + view.getSpotId())
                .coverImage(spot != null ? spot.getCoverImageUrl() : null)
                .source(view.getViewSource())
                .duration(view.getViewDuration())
                .createdAt(formatDateTime(view.getCreatedAt()))
                .build();
        }).collect(Collectors.toList());
    }

    // 查询辅助方法
    private Set<Long> resolveUserIdsByNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return null;
        }

        return userMapper.selectList(
            new LambdaQueryWrapper<User>()
                .like(User::getNickname, nickname.trim())
                .select(User::getId)
        ).stream().map(User::getId).collect(Collectors.toSet());
    }

    private Set<Long> resolveSpotIdsByName(String spotName) {
        if (spotName == null || spotName.isBlank()) {
            return null;
        }

        return spotMapper.selectList(
            new LambdaQueryWrapper<Spot>()
                .like(Spot::getName, spotName.trim())
                .select(Spot::getId)
        ).stream().map(Spot::getId).collect(Collectors.toSet());
    }

    private Long parseCategoryId(String tag) {
        try {
            return Long.parseLong(tag);
        } catch (Exception e) {
            return null;
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : DATE_TIME_FORMATTER.format(dateTime);
    }

    /**
     * 后台筛选在前置条件已经判定无结果时，统一返回空分页结构，避免散落重复代码。
     */
    private <T> PageResult<T> emptyPageResult(Integer page, Integer pageSize) {
        return PageResult.of(Collections.emptyList(), 0, page, pageSize);
    }
}
