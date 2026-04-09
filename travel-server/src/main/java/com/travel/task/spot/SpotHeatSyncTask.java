package com.travel.task.spot;

import com.travel.service.SpotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 景点热度同步任务。
 * <p>
 * 通过定时批量重算热度，保证行为累积后的热度分能周期性回写到景点表。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpotHeatSyncTask {

    // 任务只负责调度触发，具体热度计算仍收口在景点服务中。

    private final SpotService spotService;

    /**
     * 定时按热度档位和行为数据同步全部景点热度。
     */
    @Scheduled(cron = "${app.task.spot-heat-sync-cron:0 30 3 * * ?}")
    public void refreshAllSpotHeat() {
        // 任务异常只记日志，避免调度线程因为一次统计失败停止后续执行。
        log.info("定时任务：开始同步全部景点热度");
        try {
            spotService.refreshAllSpotHeat();
            log.info("定时任务：全部景点热度同步完成");
        } catch (Exception e) {
            log.error("定时任务：全部景点热度同步失败", e);
        }
    }
}
