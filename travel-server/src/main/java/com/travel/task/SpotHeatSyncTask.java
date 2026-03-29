package com.travel.task;

import com.travel.service.SpotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 景点热度同步任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpotHeatSyncTask {

    // 服务依赖

    private final SpotService spotService;

    /**
     * 定时按热度档位和行为数据同步全部景点热度。
     */
    @Scheduled(cron = "${app.task.spot-heat-sync-cron:0 30 3 * * ?}")
    public void refreshAllSpotHeat() {
        log.info("定时任务：开始同步全部景点热度");
        try {
            spotService.refreshAllSpotHeat();
            log.info("定时任务：全部景点热度同步完成");
        } catch (Exception e) {
            log.error("定时任务：全部景点热度同步失败", e);
        }
    }
}
