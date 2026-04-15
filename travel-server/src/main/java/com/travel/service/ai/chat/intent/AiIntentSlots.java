package com.travel.service.ai.chat.intent;

/**
 * AI 意图槽位 key 常量，避免各模块散落硬编码字符串。
 */
public final class AiIntentSlots {

    public static final String ORDER_NO = "orderNo";
    public static final String STATUS = "status";
    public static final String LIMIT = "limit";
    public static final String SPOT_NAME = "spotName";
    public static final String CITY = "city";
    public static final String KEYWORD = "keyword";
    public static final String BUDGET = "budget";
    public static final String DAYS = "days";
    public static final String GROUP = "group";

    private AiIntentSlots() {
    }
}
