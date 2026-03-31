package com.travel.service.impl;

import com.travel.entity.UserSpotView;
import com.travel.mapper.UserSpotViewMapper;
import com.travel.service.RecommendationService;
import com.travel.service.SpotBehaviorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 景点行为服务实现，负责浏览记录和推荐缓存失效。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpotBehaviorServiceImpl implements SpotBehaviorService {

    private final UserSpotViewMapper userSpotViewMapper;
    private final RecommendationService recommendationService;

    @Override
    public void recordView(Long spotId, Long userId, String source, Integer duration) {
        if (userId == null) {
            return;
        }
        try {
            UserSpotView view = new UserSpotView();
            view.setUserId(userId);
            view.setSpotId(spotId);
            view.setViewSource(source);
            view.setViewDuration(duration != null ? duration : 0);
            userSpotViewMapper.insert(view);
            recommendationService.invalidateUserRecommendationCache(userId);
        } catch (Exception e) {
            log.warn("记录浏览行为失败: userId={}, spotId={}", userId, spotId, e);
        }
    }
}
