package com.travel.service;

import com.travel.dto.home.HotSpotResponse;
import com.travel.dto.home.NearbySpotResponse;
import com.travel.dto.home.RecentViewedSpotResponse;
import com.travel.dto.recommendation.RecommendationConfigBundleDTO;
import com.travel.dto.recommendation.RecommendationResponse;
import com.travel.dto.recommendation.SimilarityPreviewResponse;
import com.travel.dto.recommendation.RecommendationStatusDTO;

import java.math.BigDecimal;

/**
 * 推荐服务接口。
 * <p>
 * 定义个性化推荐、热门推荐、附近景点、相似度计算和推荐配置管理能力。
 */
public interface RecommendationService {

    /**
     * 获取个性化推荐结果。
     *
     * @param userId 当前登录用户 ID
     * @param limit 返回条数上限
     * @return 推荐结果
     */
    RecommendationResponse getRecommendations(Long userId, Integer limit);

    /**
     * 刷新并重新计算推荐结果。
     *
     * @param userId 当前登录用户 ID
     * @param limit 返回条数上限
     * @return 推荐结果
     */
    RecommendationResponse refreshRecommendations(Long userId, Integer limit);

    /**
     * 管理端调试预览推荐结果。
     *
     * @param userId 用户 ID
     * @param limit 返回条数上限
     * @param refresh 是否强制刷新
     * @param debug 是否返回调试信息
     * @param stable 是否使用稳定结果
     * @return 推荐结果
     */
    RecommendationResponse previewRecommendations(Long userId, Integer limit, Boolean refresh, Boolean debug, Boolean stable);

    /**
     * 管理端预览某个景点的相似邻居。
     *
     * @param spotId 景点 ID
     * @param limit 返回条数上限
     * @return 相似邻居预览结果
     */
    SimilarityPreviewResponse previewSimilarityNeighbors(Long spotId, Integer limit);

    /**
     * 获取热门景点推荐结果。
     *
     * @param limit 返回条数上限
     * @return 热门景点结果
     */
    HotSpotResponse getHotSpots(Integer limit);

    /**
     * 获取最近都在看的景点。
     *
     * @param days 统计天数
     * @param limit 返回条数上限
     * @return 最近都在看结果
     */
    RecentViewedSpotResponse getRecentViewedSpots(Integer days, Integer limit);

    /**
     * 获取附近景点结果。
     *
     * @param latitude 当前纬度
     * @param longitude 当前经度
     * @param limit 返回条数上限
     * @return 附近景点结果
     */
    NearbySpotResponse getNearbySpots(BigDecimal latitude, BigDecimal longitude, Integer limit);

    /**
     * 计算并更新景点相似度矩阵。
     */
    void updateSimilarityMatrix();

    /**
     * 获取当前推荐配置。
     *
     * @return 推荐配置
     */
    RecommendationConfigBundleDTO getConfig();

    /**
     * 更新推荐配置。
     *
     * @param config 推荐配置
     */
    void updateConfig(RecommendationConfigBundleDTO config);

    /**
     * 清理单个用户的推荐缓存。
     *
     * @param userId 用户 ID
     */
    void invalidateUserRecommendationCache(Long userId);

    /**
     * 获取推荐引擎运行状态。
     *
     * @return 推荐引擎状态
     */
    RecommendationStatusDTO getStatus();
}
