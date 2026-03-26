package com.travel.config.cache;

/**
 * Redis Key 命名管理器。
 * <p>
 * 统一维护推荐模块相关缓存 Key 的命名规则，避免业务代码中散落硬编码字符串。
 */
public final class RedisKeyManager {

    private static final String ROOT = "waytrip";
    private static final String AI = ROOT + ":ai";
    private static final String RECOMMENDATION = ROOT + ":recommendation";
    private static final String HOME = ROOT + ":home";

    private RedisKeyManager() {
    }

    /**
     * 获取景点相似度缓存 Key。
     *
     * @param spotId 景点 ID
     * @return 景点相似度缓存 Key
     */
    public static String recommendationSimilarity(Long spotId) {
        return RECOMMENDATION + ":similarity:" + spotId;
    }

    /**
     * 获取用户推荐结果缓存 Key。
     *
     * @param userId 用户 ID
     * @return 用户推荐缓存 Key
     */
    public static String recommendationUser(Long userId) {
        return RECOMMENDATION + ":user:" + userId;
    }

    /**
     * 获取推荐算法配置缓存 Key。
     *
     * @return 推荐算法配置缓存 Key
     */
    public static String recommendationConfigAlgorithm() {
        return RECOMMENDATION + ":config:algorithm";
    }

    /**
     * 获取热度配置缓存 Key。
     *
     * @return 热度配置缓存 Key
     */
    public static String recommendationConfigHeat() {
        return RECOMMENDATION + ":config:heat";
    }

    /**
     * 获取推荐缓存配置 Key。
     *
     * @return 推荐缓存配置 Key
     */
    public static String recommendationConfigCache() {
        return RECOMMENDATION + ":config:cache";
    }

    /**
     * 获取推荐任务状态缓存 Key。
     *
     * @return 推荐状态缓存 Key
     */
    public static String recommendationStatus() {
        return RECOMMENDATION + ":status";
    }

    /**
     * 获取 AI 客服会话历史缓存 Key。
     *
     * @param sessionId 会话 ID
     * @return AI 会话历史缓存 Key
     */
    public static String aiChatSession(String sessionId) {
        return AI + ":chat:session:" + sessionId;
    }

    /**
     * 获取 AI 客服 IP 级限流缓存 Key。
     *
     * @param clientIp 客户端 IP
     * @param minuteBucket 分钟桶
     * @return AI 限流缓存 Key
     */
    public static String aiChatRateLimitIp(String clientIp, String minuteBucket) {
        return AI + ":chat:rl:ip:" + clientIp + ":" + minuteBucket;
    }

    /**
     * 获取 AI 客服会话级限流缓存 Key。
     *
     * @param sessionId 会话 ID
     * @param minuteBucket 分钟桶
     * @return AI 会话限流缓存 Key
     */
    public static String aiChatRateLimitSession(String sessionId, String minuteBucket) {
        return AI + ":chat:rl:session:" + sessionId + ":" + minuteBucket;
    }

    /**
     * 获取 AI 客服响应缓存 Key。
     *
     * @param model 模型名称
     * @param intentType 意图类型
     * @param userId 用户 ID
     * @param digest 请求摘要
     * @return AI 响应缓存 Key
     */
    public static String aiChatResponseCache(String model, String intentType, String userId, String sessionId, String digest) {
        return AI + ":chat:cache:" + model + ":" + intentType + ":" + userId + ":" + sessionId + ":" + digest;
    }

    /**
     * 获取首页热门景点缓存 Key。
     *
     * @param limit 请求条数
     * @return 热门景点缓存 Key
     */
    public static String homeHotSpots(Integer limit) {
        return HOME + ":hot:" + limit;
    }

    /**
     * 获取首页轮播图缓存 Key。
     *
     * @return 轮播图缓存 Key
     */
    public static String homeBanners() {
        return HOME + ":banners";
    }
}
