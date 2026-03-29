package com.travel.task;

import com.travel.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 推荐系统定时任务，负责周期性刷新物品相似度矩阵。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationTask {

    // 服务依赖

    private final RecommendationService recommendationService;

    /**
     * 每天凌晨 3 点重建一次物品相似度矩阵。
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void updateSimilarityMatrix() {
        log.info("定时任务：开始更新物品相似度矩阵");
        try {
            recommendationService.updateSimilarityMatrix();
            log.info("定时任务：物品相似度矩阵更新完成");
        } catch (Exception e) {
            log.error("定时任务：物品相似度矩阵更新失败", e);
        }
    }
}
