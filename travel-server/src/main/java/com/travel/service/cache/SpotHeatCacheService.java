package com.travel.service.cache;

import com.travel.config.RedisKeyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 景点热度相关缓存服务。
 */
@Service
@RequiredArgsConstructor
public class SpotHeatCacheService {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 标记指定用户在去重窗口内已浏览该景点。
     *
     * @return `true` 表示本窗口首次浏览，`false` 表示命中去重窗口
     */
    public boolean markViewInDedupeWindow(Long spotId, Long userId, long dedupeWindowMinutes) {
        if (spotId == null || userId == null || dedupeWindowMinutes <= 0) {
            return false;
        }
        Boolean firstViewInWindow = stringRedisTemplate.opsForValue().setIfAbsent(
            RedisKeyManager.spotHeatView(spotId, userId),
            "1",
            dedupeWindowMinutes,
            TimeUnit.MINUTES
        );
        return Boolean.TRUE.equals(firstViewInWindow);
    }
}
