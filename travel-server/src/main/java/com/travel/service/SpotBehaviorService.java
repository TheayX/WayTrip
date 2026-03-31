package com.travel.service;

/**
 * 景点行为服务，负责浏览等行为上报。
 */
public interface SpotBehaviorService {

    void recordView(Long spotId, Long userId, String source, Integer duration);
}
