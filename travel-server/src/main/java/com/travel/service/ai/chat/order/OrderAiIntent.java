package com.travel.service.ai.chat.order;

/**
 * 订单 AI 意图类型，用于把自然语言先归一到稳定业务动作。
 */
public enum OrderAiIntent {
    NONE,
    GUIDE_STATUS,
    GUIDE_REFUND,
    GUIDE_PAGE,
    LIST_ORDERS,
    DETAIL_BY_ORDER_NO
}
