package com.travel.config;

/**
 * Redis Key 命名管理器。
 * <p>
 * 统一维护推荐模块相关缓存 Key 的命名规则，避免业务代码中散落硬编码字符串。
 */
public final class RedisKeyManager {

    private static final String ROOT = "waytrip";
    private static final String RECOMMENDATION = ROOT + ":recommendation";

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
}
