package com.travel.task.recommendation;

import com.travel.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 推荐系统定时任务，负责周期性刷新物品相似度矩阵。
 * <p>
 * 定时任务和后台手动触发共用同一服务入口，确保矩阵重建逻辑始终只有一份实现。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationMatrixRefreshTask {

    // 推荐服务由任务直接调用，避免任务层再复制任何推荐计算逻辑。

    private final RecommendationService recommendationService;

    /**
     * 每天凌晨 3 点重建一次物品相似度矩阵。
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void updateSimilarityMatrix() {
        // 定时任务内部捕获异常，只记录日志，避免调度线程被单次失败打断。
        log.info("定时任务：开始更新物品相似度矩阵");
        try {
            recommendationService.updateSimilarityMatrix();
            log.info("定时任务：物品相似度矩阵更新完成");
        } catch (Exception e) {
            log.error("定时任务：物品相似度矩阵更新失败", e);
        }
    }
}
