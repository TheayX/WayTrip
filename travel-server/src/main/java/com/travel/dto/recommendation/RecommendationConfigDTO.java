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

    // ==================== 景点热度参数 ====================

    /** 浏览详情增加的热度分数 */
    private Integer heatViewIncrement = 1;

    /** 收藏增加的热度分数 */
    private Integer heatFavoriteIncrement = 3;

    /** 评价增加的热度分数 */
    private Integer heatReviewIncrement = 2;

    /** 支付订单增加的热度分数 */
    private Integer heatOrderPaidIncrement = 5;

    /** 同一用户同一景点浏览去重窗口（分钟） */
    private Integer heatViewDedupeWindowMinutes = 30;

    /** 最终排序时的热度轻量重排系数 */
    private Double heatRerankFactor = 0.05;

    // ==================== 算法参数 ====================

    /** 触发协同过滤的最少交互数（低于此值走冷启动） */
    private Integer minInteractionsForCF = 3;

    /** 相似度矩阵保留的近邻数量 K（论文 S(j,K)） */
    private Integer topKNeighbors = 20;

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
