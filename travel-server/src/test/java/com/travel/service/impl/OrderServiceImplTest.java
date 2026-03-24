package com.travel.service.impl;

import com.travel.entity.Order;
import com.travel.entity.Spot;
import com.travel.enums.OrderStatus;
import com.travel.mapper.OrderMapper;
import com.travel.mapper.SpotMapper;
import com.travel.mapper.UserMapper;
import com.travel.service.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private SpotMapper spotMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RecommendationService recommendationService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Spot spot;

    @BeforeEach
    void setUp() {
        spot = new Spot();
        spot.setId(100L);
        spot.setName("西湖");
        spot.setCoverImageUrl("/uploads/images/xihu.jpg");
        spot.setPrice(BigDecimal.valueOf(80));
        spot.setIsDeleted(0);
    }

    @Test
    void cancelOrder_allowsPaidOrder_andMarksItCancelled() {
        Order order = buildOrder(OrderStatus.PAID);
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(orderMapper.updateById(order)).thenReturn(1);
        when(spotMapper.selectById(order.getSpotId())).thenReturn(spot);

        var response = orderService.cancelOrder(1L, order.getId());

        assertEquals("cancelled", response.getStatus());
        assertEquals("已取消", response.getStatusText());
        assertNotNull(response.getCancelledAt());
        assertFalse(response.getCanPay());
        assertFalse(response.getCanCancel());
        verify(orderMapper).updateById(order);
    }

    @Test
    void cancelOrder_isIdempotent_forCancelledOrder() {
        Order order = buildOrder(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now().minusHours(1));
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(spotMapper.selectById(order.getSpotId())).thenReturn(spot);

        var response = orderService.cancelOrder(1L, order.getId());

        assertEquals("cancelled", response.getStatus());
        assertNotNull(response.getCancelledAt());
        assertFalse(response.getCanCancel());
    }

    @Test
    void completeOrder_rejectsNonPaidOrder() {
        Order order = buildOrder(OrderStatus.PENDING);
        when(orderMapper.selectById(order.getId())).thenReturn(order);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> orderService.completeOrder(order.getId()));

        assertEquals("订单状态不允许完成", ex.getMessage());
    }

    @Test
    void reopenOrder_restoresCompletedOrderToPaid() {
        Order order = buildOrder(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now().minusMinutes(10));
        when(orderMapper.selectById(order.getId())).thenReturn(order);
        when(orderMapper.update(any(), any())).thenReturn(1);
        when(spotMapper.selectById(order.getSpotId())).thenReturn(spot);

        var response = orderService.reopenOrder(order.getId());

        assertEquals("paid", response.getStatus());
        assertEquals("已支付", response.getStatusText());
        assertNull(response.getCompletedAt());
        assertFalse(response.getCanPay());
        assertTrue(response.getCanCancel());
    }

    private Order buildOrder(OrderStatus status) {
        Order order = new Order();
        order.setId(10L);
        order.setOrderNo("T202603220001");
        order.setUserId(1L);
        order.setSpotId(spot.getId());
        order.setQuantity(2);
        order.setTotalAmount(BigDecimal.valueOf(160));
        order.setStatus(status.getCode());
        order.setVisitDate(LocalDate.now().plusDays(3));
        order.setContactName("测试用户");
        order.setContactPhone("13800138000");
        order.setIsDeleted(0);
        order.setCreatedAt(LocalDateTime.now().minusHours(2));
        order.setUpdatedAt(LocalDateTime.now().minusHours(2));
        if (status == OrderStatus.PAID || status == OrderStatus.COMPLETED) {
            order.setPaidAt(LocalDateTime.now().minusHours(1));
        }
        return order;
    }
}
