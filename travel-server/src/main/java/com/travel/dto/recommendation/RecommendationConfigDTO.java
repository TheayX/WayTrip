package com.travel.dto.recommendation;

import lombok.Data;

/**
 * 推荐算法配置 DTO
 */
@Data
public class RecommendationConfigDTO {

    // ==================== 交互权重（论文 rui） ====================

    /** 浏览行为权重 */
    private Double weightView = 0.5;

    /** 收藏行为权重 */
    private Double weightFavorite = 1.0;

    /** 评分因子（实际权重 = 评分 × 因子） */
    private Double weightReviewFactor = 0.4;

    /** 已付款订单权重 */
    private Double weightOrderPaid = 3.0;

    /** 已完成订单权重 */
    private Double weightOrderCompleted = 4.0;

    /** 首页进入浏览来源因子 */
    private Double viewSourceFactorHome = 0.9;

    /** 搜索进入浏览来源因子 */
    private Double viewSourceFactorSearch = 1.2;

    /** 推荐位进入浏览来源因子 */
    private Double viewSourceFactorRecommend = 1.1;

    /** 攻略进入浏览来源因子 */
    private Double viewSourceFactorGuide = 1.0;

    /** 详情页二跳等默认浏览来源因子 */
    private Double viewSourceFactorDetail = 1.0;

    /** 短停留阈值（秒） */
    private Integer viewDurationShortThresholdSeconds = 10;

    /** 中停留阈值（秒） */
    private Integer viewDurationMediumThresholdSeconds = 60;

    /** 长停留阈值（秒） */
    private Integer viewDurationLongThresholdSeconds = 180;

    /** 短停留因子 */
    private Double viewDurationFactorShort = 0.6;

    /** 普通停留因子 */
    private Double viewDurationFactorMedium = 1.0;

    /** 较长停留因子 */
    private Double viewDurationFactorLong = 1.2;

    /** 超长停留因子 */
    private Double viewDurationFactorVeryLong = 1.35;

    // ==================== 景点热度参数 ====================

    /** 浏览详情增加的热度分数 */
    private Integer heatViewIncrement = 1;

    /** 收藏增加的热度分数 */
    private Integer heatFavoriteIncrement = 3;

    /** 评价增加的热度分数 */
    private Integer heatReviewIncrement = 2;

    /** 支付订单增加的热度分数 */
    private Integer heatOrderPaidIncrement = 5;

    /** 完成订单增加的热度分数 */
    private Integer heatOrderCompletedIncrement = 8;

    /** 同一用户同一景点浏览去重窗口（分钟） */
    private Integer heatViewDedupeWindowMinutes = 30;

    /** 最终排序时的热度轻量重排系数 */
    private Double heatRerankFactor = 0.05;

    // ==================== 算法参数 ====================

    /** 触发协同过滤的最少交互数（低于此值走冷启动） */
    private Integer minInteractionsForCF = 3;

    /** 相似度矩阵保留的近邻数量 K（论文 S(j,K)） */
    private Integer topKNeighbors = 20;

    /** 个性化推荐计算时的候选扩容倍数 */
    private Integer candidateExpandFactor = 2;

    /** 冷启动刷新时的候选扩容倍数 */
    private Integer coldStartExpandFactor = 3;

    // ==================== 缓存参数 ====================

    /** 相似度矩阵缓存时长（小时） */
    private Integer similarityTTLHours = 24;

    /** 用户推荐缓存时长（分钟） */
    private Integer userRecTTLMinutes = 60;

    /**
     * 返回一份默认配置
     */
    public static RecommendationConfigDTO defaultConfig() {
        return new RecommendationConfigDTO();
    }
}
