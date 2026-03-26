package com.travel.dto.recommendation;

import lombok.Data;

/**
 * 聚合后的推荐配置。
 */
@Data
public class RecommendationConfigBundleDTO {

    private RecommendationAlgorithmConfigDTO algorithm = new RecommendationAlgorithmConfigDTO();
    private RecommendationHeatConfigDTO heat = new RecommendationHeatConfigDTO();
    private RecommendationCacheConfigDTO cache = new RecommendationCacheConfigDTO();

    public static RecommendationConfigBundleDTO defaultConfig() {
        return new RecommendationConfigBundleDTO();
    }

    public static RecommendationConfigBundleDTO fromLegacy(LegacyRecommendationConfigDTO legacy) {
        LegacyRecommendationConfigDTO source = legacy == null ? LegacyRecommendationConfigDTO.defaultConfig() : legacy;
        RecommendationConfigBundleDTO bundle = new RecommendationConfigBundleDTO();

        RecommendationAlgorithmConfigDTO algorithm = new RecommendationAlgorithmConfigDTO();
        algorithm.setWeightView(source.getWeightView());
        algorithm.setWeightFavorite(source.getWeightFavorite());
        algorithm.setWeightReviewFactor(source.getWeightReviewFactor());
        algorithm.setWeightOrderPaid(source.getWeightOrderPaid());
        algorithm.setWeightOrderCompleted(source.getWeightOrderCompleted());
        algorithm.setViewSourceFactorHome(source.getViewSourceFactorHome());
        algorithm.setViewSourceFactorSearch(source.getViewSourceFactorSearch());
        algorithm.setViewSourceFactorRecommend(source.getViewSourceFactorRecommend());
        algorithm.setViewSourceFactorGuide(source.getViewSourceFactorGuide());
        algorithm.setViewSourceFactorDetail(source.getViewSourceFactorDetail());
        algorithm.setViewDurationShortThresholdSeconds(source.getViewDurationShortThresholdSeconds());
        algorithm.setViewDurationMediumThresholdSeconds(source.getViewDurationMediumThresholdSeconds());
        algorithm.setViewDurationLongThresholdSeconds(source.getViewDurationLongThresholdSeconds());
        algorithm.setViewDurationFactorShort(source.getViewDurationFactorShort());
        algorithm.setViewDurationFactorMedium(source.getViewDurationFactorMedium());
        algorithm.setViewDurationFactorLong(source.getViewDurationFactorLong());
        algorithm.setViewDurationFactorVeryLong(source.getViewDurationFactorVeryLong());
        algorithm.setMinInteractionsForCF(source.getMinInteractionsForCF());
        algorithm.setTopKNeighbors(source.getTopKNeighbors());
        algorithm.setCandidateExpandFactor(source.getCandidateExpandFactor());
        algorithm.setColdStartExpandFactor(source.getColdStartExpandFactor());

        RecommendationHeatConfigDTO heat = new RecommendationHeatConfigDTO();
        heat.setHeatViewIncrement(source.getHeatViewIncrement());
        heat.setHeatFavoriteIncrement(source.getHeatFavoriteIncrement());
        heat.setHeatReviewIncrement(source.getHeatReviewIncrement());
        heat.setHeatOrderPaidIncrement(source.getHeatOrderPaidIncrement());
        heat.setHeatOrderCompletedIncrement(source.getHeatOrderCompletedIncrement());
        heat.setHeatViewDedupeWindowMinutes(source.getHeatViewDedupeWindowMinutes());
        heat.setHeatRerankFactor(source.getHeatRerankFactor());

        RecommendationCacheConfigDTO cache = new RecommendationCacheConfigDTO();
        cache.setSimilarityTTLHours(source.getSimilarityTTLHours());
        cache.setUserRecTTLMinutes(source.getUserRecTTLMinutes());

        bundle.setAlgorithm(algorithm);
        bundle.setHeat(heat);
        bundle.setCache(cache);
        return bundle;
    }

    public LegacyRecommendationConfigDTO toLegacy() {
        LegacyRecommendationConfigDTO legacy = LegacyRecommendationConfigDTO.defaultConfig();

        RecommendationAlgorithmConfigDTO algorithmConfig = algorithm == null ? new RecommendationAlgorithmConfigDTO() : algorithm;
        legacy.setWeightView(algorithmConfig.getWeightView());
        legacy.setWeightFavorite(algorithmConfig.getWeightFavorite());
        legacy.setWeightReviewFactor(algorithmConfig.getWeightReviewFactor());
        legacy.setWeightOrderPaid(algorithmConfig.getWeightOrderPaid());
        legacy.setWeightOrderCompleted(algorithmConfig.getWeightOrderCompleted());
        legacy.setViewSourceFactorHome(algorithmConfig.getViewSourceFactorHome());
        legacy.setViewSourceFactorSearch(algorithmConfig.getViewSourceFactorSearch());
        legacy.setViewSourceFactorRecommend(algorithmConfig.getViewSourceFactorRecommend());
        legacy.setViewSourceFactorGuide(algorithmConfig.getViewSourceFactorGuide());
        legacy.setViewSourceFactorDetail(algorithmConfig.getViewSourceFactorDetail());
        legacy.setViewDurationShortThresholdSeconds(algorithmConfig.getViewDurationShortThresholdSeconds());
        legacy.setViewDurationMediumThresholdSeconds(algorithmConfig.getViewDurationMediumThresholdSeconds());
        legacy.setViewDurationLongThresholdSeconds(algorithmConfig.getViewDurationLongThresholdSeconds());
        legacy.setViewDurationFactorShort(algorithmConfig.getViewDurationFactorShort());
        legacy.setViewDurationFactorMedium(algorithmConfig.getViewDurationFactorMedium());
        legacy.setViewDurationFactorLong(algorithmConfig.getViewDurationFactorLong());
        legacy.setViewDurationFactorVeryLong(algorithmConfig.getViewDurationFactorVeryLong());
        legacy.setMinInteractionsForCF(algorithmConfig.getMinInteractionsForCF());
        legacy.setTopKNeighbors(algorithmConfig.getTopKNeighbors());
        legacy.setCandidateExpandFactor(algorithmConfig.getCandidateExpandFactor());
        legacy.setColdStartExpandFactor(algorithmConfig.getColdStartExpandFactor());

        RecommendationHeatConfigDTO heatConfig = heat == null ? new RecommendationHeatConfigDTO() : heat;
        legacy.setHeatViewIncrement(heatConfig.getHeatViewIncrement());
        legacy.setHeatFavoriteIncrement(heatConfig.getHeatFavoriteIncrement());
        legacy.setHeatReviewIncrement(heatConfig.getHeatReviewIncrement());
        legacy.setHeatOrderPaidIncrement(heatConfig.getHeatOrderPaidIncrement());
        legacy.setHeatOrderCompletedIncrement(heatConfig.getHeatOrderCompletedIncrement());
        legacy.setHeatViewDedupeWindowMinutes(heatConfig.getHeatViewDedupeWindowMinutes());
        legacy.setHeatRerankFactor(heatConfig.getHeatRerankFactor());

        RecommendationCacheConfigDTO cacheConfig = cache == null ? new RecommendationCacheConfigDTO() : cache;
        legacy.setSimilarityTTLHours(cacheConfig.getSimilarityTTLHours());
        legacy.setUserRecTTLMinutes(cacheConfig.getUserRecTTLMinutes());

        return legacy;
    }
}
