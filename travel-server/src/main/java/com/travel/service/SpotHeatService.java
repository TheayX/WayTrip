package com.travel.service;

/**
 * 景点热度服务，负责热度分同步。
 * <p>
 * 热度重算独立成服务，便于后台手动触发和定时任务复用同一套规则。
 */
public interface SpotHeatService {

    /**
     * 重算单个景点热度。
     *
     * @param spotId 景点 ID
     */
    void refreshSpotHeat(Long spotId);

    /**
     * 重算全部景点热度。
     */
    void refreshAllSpotHeat();
}
