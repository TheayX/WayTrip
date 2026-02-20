package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.dto.user.*;
import com.travel.entity.*;
import com.travel.mapper.*;
import com.travel.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final OrderMapper orderMapper;
    private final FavoriteMapper favoriteMapper;
    private final RatingMapper ratingMapper;
    private final SpotMapper spotMapper;

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
        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new RuntimeException("用户不存在");
        }

        AdminUserDetailResponse response = new AdminUserDetailResponse();
        response.setId(user.getId());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        response.setPhone(user.getPhone());
        response.setPreferences(user.getPreferences());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());

        // 统计数据
        response.setOrderCount(Math.toIntExact(orderMapper.selectCount(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getIsDeleted, 0)
        )));
        response.setFavoriteCount(Math.toIntExact(favoriteMapper.selectCount(
            new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getIsDeleted, 0)
        )));
        response.setRatingCount(Math.toIntExact(ratingMapper.selectCount(
            new LambdaQueryWrapper<Rating>()
                .eq(Rating::getUserId, userId)
                .eq(Rating::getIsDeleted, 0)
        )));

        // 最近订单
        List<Order> recentOrders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getIsDeleted, 0)
                .orderByDesc(Order::getCreatedAt)
                .last("LIMIT 5")
        );

        // 获取景点名称
        java.util.Set<Long> spotIds = recentOrders.stream()
            .map(Order::getSpotId)
            .collect(java.util.stream.Collectors.toSet());
        java.util.Map<Long, String> spotNameMap = new java.util.HashMap<>();
        if (!spotIds.isEmpty()) {
            spotMapper.selectBatchIds(spotIds).forEach(spot -> 
                spotNameMap.put(spot.getId(), spot.getName())
            );
        }

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

    private String convertStatusToString(Integer status) {
        if (status == null) return "unknown";
        return switch (status) {
            case Order.STATUS_PENDING -> "pending";
            case Order.STATUS_PAID -> "paid";
            case Order.STATUS_CANCELLED -> "cancelled";
            case Order.STATUS_REFUNDED -> "refunded";
            case Order.STATUS_COMPLETED -> "completed";
            default -> "unknown";
        };
    }

    private AdminUserListResponse.UserItem buildUserItem(User user) {
        AdminUserListResponse.UserItem item = new AdminUserListResponse.UserItem();
        item.setId(user.getId());
        item.setNickname(user.getNickname());
        item.setAvatar(user.getAvatar());
        item.setPhone(user.getPhone());
        item.setCreatedAt(user.getCreatedAt());
        item.setUpdatedAt(user.getUpdatedAt());

        // 统计数据
        item.setOrderCount(Math.toIntExact(orderMapper.selectCount(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, user.getId())
                .eq(Order::getIsDeleted, 0)
        )));
        item.setFavoriteCount(Math.toIntExact(favoriteMapper.selectCount(
            new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, user.getId())
                .eq(Favorite::getIsDeleted, 0)
        )));
        item.setRatingCount(Math.toIntExact(ratingMapper.selectCount(
            new LambdaQueryWrapper<Rating>()
                .eq(Rating::getUserId, user.getId())
                .eq(Rating::getIsDeleted, 0)
        )));

        return item;
    }
}
