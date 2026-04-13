package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.entity.Spot;
import com.travel.entity.User;
import com.travel.entity.UserSpotView;
import com.travel.mapper.SpotMapper;
import com.travel.mapper.UserMapper;
import com.travel.mapper.UserSpotViewMapper;
import com.travel.service.RecommendationService;
import com.travel.service.SpotBehaviorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 景点行为服务实现，负责浏览记录和推荐缓存失效。
 * <p>
 * 行为落库失败不应阻断主业务浏览流程，因此这里采用记录失败仅告警的策略。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpotBehaviorServiceImpl implements SpotBehaviorService {

    private final UserSpotViewMapper userSpotViewMapper;
    private final UserMapper userMapper;
    private final SpotMapper spotMapper;
    private final RecommendationService recommendationService;

    @Override
    public void recordView(Long spotId, Long userId, String source, Integer duration) {
        // 未登录用户不记录私有行为，避免产生无法归属的浏览数据。
        if (userId == null) {
            return;
        }
        try {
            if (!isActiveUser(userId) || !isActiveSpot(spotId)) {
                return;
            }
            UserSpotView view = new UserSpotView();
            view.setUserId(userId);
            view.setSpotId(spotId);
            view.setViewSource(source);
            view.setViewDuration(duration != null ? duration : 0);
            userSpotViewMapper.insert(view);
            // 浏览行为会影响推荐结果，写入后立即让当前用户推荐缓存失效。
            recommendationService.invalidateUserRecommendationCache(userId);
        } catch (Exception e) {
            log.warn("记录浏览行为失败: userId={}, spotId={}", userId, spotId, e);
        }
    }

    /**
     * 浏览行为表取消外键后，落库前先过滤掉已失效的用户引用。
     */
    private boolean isActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null && user.getIsDeleted() == 0;
    }

    /**
     * 浏览记录只保留仍然存在的景点，避免行为日志出现悬挂主键。
     */
    private boolean isActiveSpot(Long spotId) {
        Spot spot = spotMapper.selectOne(
            new LambdaQueryWrapper<Spot>()
                .eq(Spot::getId, spotId)
                .eq(Spot::getIsDeleted, 0)
        );
        return spot != null;
    }
}
