package com.travel.task.order;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.travel.entity.Order;
import com.travel.enums.OrderStatus;
import com.travel.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 订单自动取消任务，负责清理超时未支付订单。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutoCancelTask {

    // 超时阈值配置
    private static final int TIMEOUT_MINUTES = 5;

    // 持久层依赖
    private final OrderMapper orderMapper;

    /**
     * 每分钟扫描一次，取消超时未支付订单。
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void cancelTimeoutOrders() {
        // 将创建时间早于阈值的待支付订单统一更新为已取消。
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);
        UpdateWrapper<Order> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("status", OrderStatus.PENDING.getCode())
                .eq("is_deleted", 0)
                .le("created_at", cutoff)
                .set("status", OrderStatus.CANCELLED.getCode())
                .set("cancelled_at", LocalDateTime.now())
                .set("updated_at", LocalDateTime.now());

        int updated = orderMapper.update(null, updateWrapper);
        if (updated > 0) {
            log.info("自动取消超时未支付订单，数量={}", updated);
        }
    }
}
