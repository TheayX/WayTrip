package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.dto.user.request.AdminUserListRequest;
import com.travel.dto.user.request.ResetUserPasswordRequest;
import com.travel.dto.user.response.AdminUserDetailResponse;
import com.travel.dto.user.response.AdminUserListResponse;
import com.travel.entity.*;
import com.travel.enums.OrderStatus;
import com.travel.mapper.*;
import com.travel.service.UserProfileService;
import com.travel.util.mask.MaskUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理服务实现，负责管理端用户查询、详情统计与密码重置。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    // 持久层依赖

    private final UserMapper userMapper;
    private final OrderMapper orderMapper;
    private final UserSpotFavoriteMapper userSpotFavoriteMapper;
    private final UserSpotViewMapper userSpotViewMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final ReviewMapper reviewMapper;
    private final SpotMapper spotMapper;
    private final SpotCategoryMapper spotCategoryMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    // 管理端用户查询与维护

    @Override
    public AdminUserListResponse getAdminUsers(AdminUserListRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getIsDeleted, 0);
        
        if (StringUtils.hasText(request.getNickname())) {
            wrapper.like(User::getNickname, request.getNickname());
        }
        
        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> page = new Page<>(request.getPage(), request.getPageSize());
        Page<User> result = userMapper.selectPage(page, wrapper);

        AdminUserListResponse response = new AdminUserListResponse();
        response.setList(result.getRecords().stream()
            .map(this::buildUserItem)
            .collect(Collectors.toList()));
        response.setTotal(result.getTotal());
        response.setPage(request.getPage());
        response.setPageSize(request.getPageSize());

        return response;
    }

    @Override
    public AdminUserDetailResponse getAdminUserDetail(Long userId) {
        User user = getExistingUser(userId);

        AdminUserDetailResponse response = new AdminUserDetailResponse();
        response.setId(user.getId());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatarUrl());
        response.setPhone(MaskUtils.maskPhone(user.getPhone()));
        response.setPreferences(user.getPreferences());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());

        // 详情页需要聚合订单、收藏和评价数量。
        response.setOrderCount(countOrders(userId));
        response.setFavoriteCount(countFavorites(userId));
        response.setRatingCount(countRatings(userId));
        response.setViewCount(countViews(userId));

        response.setPreferenceSummary(buildPreferenceSummary(userId));
        response.setFavoriteSummary(buildFavoriteSummary(userId));
        response.setViewSummary(buildViewSummary(userId));

        // 详情页仅展示最近订单摘要，避免一次性返回过多记录。
        List<Order> recentOrders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getIsDeleted, 0)
                .orderByDesc(Order::getCreatedAt)
                .last("LIMIT 5")
        );

        // 订单表中仅保存景点 ID，展示时补充景点名称。
        java.util.Map<Long, String> spotNameMap = buildSpotNameMap(
            recentOrders.stream().map(Order::getSpotId).collect(java.util.stream.Collectors.toSet())
        );

        response.setRecentOrders(recentOrders.stream().map(order -> {
            AdminUserDetailResponse.RecentOrder ro = new AdminUserDetailResponse.RecentOrder();
            ro.setId(order.getId());
            ro.setOrderNo(order.getOrderNo());
            ro.setSpotName(spotNameMap.get(order.getSpotId()));
            ro.setStatus(convertStatusToString(order.getStatus()));
            ro.setCreatedAt(order.getCreatedAt());
            ro.setUpdatedAt(order.getUpdatedAt());
            return ro;
        }).collect(Collectors.toList()));

        return response;
    }

    private AdminUserDetailResponse.PreferenceSummary buildPreferenceSummary(Long userId) {
        List<UserPreference> preferences = userPreferenceMapper.selectList(
            new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getUserId, userId)
                .eq(UserPreference::getIsDeleted, 0)
                .orderByDesc(UserPreference::getUpdatedAt)
        );
        if (preferences.isEmpty()) {
            return null;
        }

        java.util.Set<Long> categoryIds = preferences.stream()
            .map(UserPreference::getTag)
            .map(this::parseCategoryId)
            .filter(java.util.Objects::nonNull)
            .collect(java.util.stream.Collectors.toSet());
        java.util.Map<Long, String> categoryNameMap = categoryIds.isEmpty()
            ? java.util.Collections.emptyMap()
            : spotCategoryMapper.selectBatchIds(categoryIds).stream()
                .collect(java.util.stream.Collectors.toMap(SpotCategory::getId, SpotCategory::getName));

        AdminUserDetailResponse.PreferenceSummary summary = new AdminUserDetailResponse.PreferenceSummary();
        summary.setCount(preferences.size());
        summary.setTags(preferences.stream()
            .map(UserPreference::getTag)
            .map(this::parseCategoryId)
            .filter(java.util.Objects::nonNull)
            .map(categoryNameMap::get)
            .filter(StringUtils::hasText)
            .distinct()
            .limit(4)
            .collect(Collectors.toList()));
        summary.setUpdatedAt(preferences.stream()
            .map(UserPreference::getUpdatedAt)
            .filter(java.util.Objects::nonNull)
            .max(LocalDateTime::compareTo)
            .orElse(null));
        return summary;
    }

    // 用户行为摘要构建
    private AdminUserDetailResponse.FavoriteSummary buildFavoriteSummary(Long userId) {
        UserSpotFavorite latestFavorite = userSpotFavoriteMapper.selectOne(
            new LambdaQueryWrapper<UserSpotFavorite>()
                .eq(UserSpotFavorite::getUserId, userId)
                .eq(UserSpotFavorite::getIsDeleted, 0)
                .orderByDesc(UserSpotFavorite::getCreatedAt)
                .last("LIMIT 1")
        );
        if (latestFavorite == null) {
            return null;
        }

        Spot latestSpot = spotMapper.selectById(latestFavorite.getSpotId());
        AdminUserDetailResponse.FavoriteSummary summary = new AdminUserDetailResponse.FavoriteSummary();
        summary.setLatestSpotName(latestSpot != null ? latestSpot.getName() : "景点#" + latestFavorite.getSpotId());
        summary.setLatestCreatedAt(latestFavorite.getCreatedAt());
        return summary;
    }

    private AdminUserDetailResponse.ViewSummary buildViewSummary(Long userId) {
        List<UserSpotView> views = userSpotViewMapper.selectList(
            new LambdaQueryWrapper<UserSpotView>()
                .eq(UserSpotView::getUserId, userId)
                .orderByDesc(UserSpotView::getCreatedAt)
                .last("LIMIT 20")
        );
        if (views.isEmpty()) {
            return null;
        }

        UserSpotView latestView = views.get(0);
        Spot latestSpot = spotMapper.selectById(latestView.getSpotId());

        java.util.Map<String, Long> sourceCounter = views.stream()
            .filter(view -> StringUtils.hasText(view.getViewSource()))
            .collect(Collectors.groupingBy(UserSpotView::getViewSource, Collectors.counting()));

        int averageDuration = (int) Math.round(views.stream()
            .map(UserSpotView::getViewDuration)
            .filter(java.util.Objects::nonNull)
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0));

        AdminUserDetailResponse.ViewSummary summary = new AdminUserDetailResponse.ViewSummary();
        summary.setLatestSpotName(latestSpot != null ? latestSpot.getName() : "景点#" + latestView.getSpotId());
        summary.setLatestCreatedAt(latestView.getCreatedAt());
        summary.setTopSource(sourceCounter.entrySet().stream()
            .max(java.util.Map.Entry.comparingByValue())
            .map(java.util.Map.Entry::getKey)
            .orElse(null));
        summary.setAverageDuration(averageDuration);
        return summary;
    }

    // 内部辅助方法
    private String convertStatusToString(Integer status) {
        OrderStatus orderStatus = OrderStatus.fromCode(status);
        return orderStatus != null ? orderStatus.getKey() : "unknown";
    }

    /**
     * 用户详情和密码重置都要求目标用户必须存在，统一收口存在性校验。
     */
    private User getExistingUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new RuntimeException("用户不存在");
        }
        return user;
    }

    private int countOrders(Long userId) {
        return Math.toIntExact(orderMapper.selectCount(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getIsDeleted, 0)
        ));
    }

    private int countFavorites(Long userId) {
        return Math.toIntExact(userSpotFavoriteMapper.selectCount(
            new LambdaQueryWrapper<UserSpotFavorite>()
                .eq(UserSpotFavorite::getUserId, userId)
                .eq(UserSpotFavorite::getIsDeleted, 0)
        ));
    }

    private int countRatings(Long userId) {
        return Math.toIntExact(reviewMapper.selectCount(
            new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getIsDeleted, 0)
        ));
    }

    private int countViews(Long userId) {
        return Math.toIntExact(userSpotViewMapper.selectCount(
            new LambdaQueryWrapper<UserSpotView>()
                .eq(UserSpotView::getUserId, userId)
        ));
    }

    /**
     * 最近订单摘要只需要景点名称，统一按景点 ID 批量补齐，避免零散查询。
     */
    private java.util.Map<Long, String> buildSpotNameMap(java.util.Set<Long> spotIds) {
        java.util.Map<Long, String> spotNameMap = new java.util.HashMap<>();
        if (!spotIds.isEmpty()) {
            spotMapper.selectBatchIds(spotIds).forEach(spot -> spotNameMap.put(spot.getId(), spot.getName()));
        }
        return spotNameMap;
    }

    private Long parseCategoryId(String tag) {
        try {
            return Long.parseLong(tag);
        } catch (Exception e) {
            return null;
        }
    }

    // 内部转换方法

    private AdminUserListResponse.UserItem buildUserItem(User user) {
        AdminUserListResponse.UserItem item = new AdminUserListResponse.UserItem();
        item.setId(user.getId());
        item.setNickname(user.getNickname());
        item.setAvatar(user.getAvatarUrl());
        item.setPhone(MaskUtils.maskPhone(user.getPhone()));
        item.setCreatedAt(user.getCreatedAt());
        item.setUpdatedAt(user.getUpdatedAt());

        // 列表页同步返回关键统计项，减少前端二次查询。
        item.setOrderCount(countOrders(user.getId()));
        item.setFavoriteCount(countFavorites(user.getId()));
        item.setRatingCount(countRatings(user.getId()));

        return item;
    }

    @Override
    public void resetUserPassword(Long userId, ResetUserPasswordRequest request) {
        User user = getExistingUser(userId);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
        log.info("用户密码已重置: userId={}", userId);
    }
}
