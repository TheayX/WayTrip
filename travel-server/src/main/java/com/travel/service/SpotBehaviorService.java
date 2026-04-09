package com.travel.service;

/**
 * 景点行为服务，负责浏览等行为上报。
 * <p>
 * 行为记录与景点查询分离后，后续扩展更多行为类型时不会挤占查询服务职责。
 */
public interface SpotBehaviorService {

    /**
     * 记录浏览行为。
     *
     * @param spotId 景点 ID
     * @param userId 用户 ID
     * @param source 浏览来源
     * @param duration 停留时长
     */
    void recordView(Long spotId, Long userId, String source, Integer duration);
}
