package com.travel.task;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.travel.entity.Order;
import com.travel.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 订单超时自动取消任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutoCancelTask {

    private static final int TIMEOUT_MINUTES = 5;

    private final OrderMapper orderMapper;

    /**
     * 每分钟扫描一次，取消超时未支付订单
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void cancelTimeoutOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);
        UpdateWrapper<Order> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("status", Order.STATUS_PENDING)
                .eq("is_deleted", 0)
                .le("created_at", cutoff)
                .set("status", Order.STATUS_CANCELLED)
                .set("cancelled_at", LocalDateTime.now());

        int updated = orderMapper.update(null, updateWrapper);
        if (updated > 0) {
            log.info("自动取消超时未支付订单，数量={}", updated);
        }
    }
}
