package com.travel.service;

/**
 * 景点热度服务，负责热度分同步。
 */
public interface SpotHeatService {

    void refreshSpotHeat(Long spotId);

    void refreshAllSpotHeat();
}
