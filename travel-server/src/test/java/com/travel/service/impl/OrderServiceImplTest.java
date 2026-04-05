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
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 订单服务测试
 * 重点覆盖取消订单、完成订单和重新打开订单的状态流转。
 */
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

    /**
     * 构建基础景点夹具，供订单响应断言使用。
     */
    @BeforeEach
    void setUp() {
        spot = new Spot();
        spot.setId(100L);
        spot.setName("西湖");
        spot.setCoverImageUrl("/uploads/spot/default/cover/default.jpg");
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

    @Test
    void getOrderDetail_autoCancelsTimeoutPendingOrder() {
        Order order = buildOrder(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now().minusMinutes(6));
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(orderMapper.updateById(order)).thenReturn(1);
        when(spotMapper.selectById(order.getSpotId())).thenReturn(spot);

        var response = orderService.getOrderDetail(1L, order.getId());

        assertEquals("cancelled", response.getStatus());
        assertEquals("已取消", response.getStatusText());
        assertNotNull(response.getCancelledAt());
        assertFalse(response.getCanPay());
        verify(orderMapper).updateById(order);
    }

    @Test
    void getUserOrders_autoCancelsTimeoutPendingOrdersInCurrentPage() {
        Order timeoutOrder = buildOrder(OrderStatus.PENDING);
        timeoutOrder.setCreatedAt(LocalDateTime.now().minusMinutes(6));
        Order freshOrder = buildOrder(OrderStatus.PENDING);
        freshOrder.setId(11L);
        freshOrder.setOrderNo("T202603220002");
        freshOrder.setCreatedAt(LocalDateTime.now().minusMinutes(2));

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Order> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        page.setRecords(List.of(timeoutOrder, freshOrder));
        page.setTotal(2);

        when(orderMapper.selectPage(any(), any())).thenReturn(page);
        when(orderMapper.updateById(timeoutOrder)).thenReturn(1);
        when(spotMapper.selectBatchIds(any())).thenReturn(List.of(spot));

        var response = orderService.getUserOrders(1L, new com.travel.dto.order.request.OrderListRequest());

        assertEquals(2, response.getList().size());
        assertEquals("cancelled", response.getList().get(0).getStatus());
        assertEquals("pending", response.getList().get(1).getStatus());
        verify(orderMapper).updateById(timeoutOrder);
        verify(orderMapper, never()).updateById(freshOrder);
    }

    @Test
    void payOrder_rejectsTimeoutPendingOrderWithExplicitMessage() {
        Order order = buildOrder(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now().minusMinutes(6));
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(orderMapper.updateById(order)).thenReturn(1);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> orderService.payOrder(1L, order.getId(), "idem-1"));

        assertEquals("订单已超时，已自动取消", ex.getMessage());
        verify(orderMapper).updateById(order);
    }

    @Test
    void getOrderDetail_autoCancelsPendingOrderAtExactTimeoutBoundary() {
        Order order = buildOrder(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(orderMapper.updateById(order)).thenReturn(1);
        when(spotMapper.selectById(order.getSpotId())).thenReturn(spot);

        var response = orderService.getOrderDetail(1L, order.getId());

        assertEquals("cancelled", response.getStatus());
        assertNotNull(response.getCancelledAt());
        verify(orderMapper).updateById(order);
    }

    /**
     * 按指定状态构造订单夹具。
     */
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
