package com.travel.service.support.recommendation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.dto.recommendation.config.RecommendationAlgorithmConfigDTO;
import com.travel.dto.recommendation.response.RecommendationResponse;
import com.travel.entity.Order;
import com.travel.entity.Review;
import com.travel.entity.UserSpotFavorite;
import com.travel.entity.UserSpotView;
import com.travel.enums.OrderStatus;
import com.travel.mapper.OrderMapper;
import com.travel.mapper.ReviewMapper;
import com.travel.mapper.UserSpotFavoriteMapper;
import com.travel.mapper.UserSpotViewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 推荐调试支撑，集中处理调试信息组装和调试日志输出。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationDebugSupport {

    private final UserSpotViewMapper userSpotViewMapper;
    private final UserSpotFavoriteMapper userSpotFavoriteMapper;
    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final RecommendationMetadataSupport recommendationMetadataSupport;
    private final RecommendationInteractionSupport recommendationInteractionSupport;

    /**
     * 初始化本次预览或调试请求的基础上下文，后续所有调试字段都挂在这里。
     */
    public RecommendationResponse.DebugInfo initDebugInfo(Long userId, Integer limit, boolean refresh) {
        RecommendationResponse.DebugInfo debugInfo = new RecommendationResponse.DebugInfo();
        debugInfo.setUserId(userId);
        debugInfo.setRequestLimit(limit);
        debugInfo.setRefresh(refresh);
        debugInfo.setDebugEnabled(true);
        return debugInfo;
    }

    /**
     * 回填融合后的交互权重明细，便于核对用户行为是否被正确纳入推荐输入。
     */
    public void populateInteractionDebugInfo(RecommendationResponse.DebugInfo debugInfo, Map<Long, Double> userInteractions) {
        if (debugInfo == null) {
            return;
        }
        debugInfo.setInteractionCount(userInteractions.size());
        debugInfo.setUserInteractions(toDebugEntries(userInteractions, "用户对该景点的融合交互权重"));
    }

    /**
     * 汇总四类行为的原始条数和去重后的景点数，帮助判断为何会触发冷启动。
     */
    public void populateBehaviorStats(RecommendationResponse.DebugInfo debugInfo, Long userId) {
        if (debugInfo == null || userId == null) {
            return;
        }

        List<UserSpotView> views = userSpotViewMapper.selectList(
            new LambdaQueryWrapper<UserSpotView>().eq(UserSpotView::getUserId, userId).select(UserSpotView::getSpotId)
        );
        List<UserSpotFavorite> favorites = userSpotFavoriteMapper.selectList(
            new LambdaQueryWrapper<UserSpotFavorite>()
                .eq(UserSpotFavorite::getUserId, userId)
                .eq(UserSpotFavorite::getIsDeleted, 0)
                .select(UserSpotFavorite::getSpotId)
        );
        List<Review> reviews = reviewMapper.selectList(
            new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getIsDeleted, 0)
                .select(Review::getSpotId)
        );
        List<Order> orders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getIsDeleted, 0)
                .in(Order::getStatus, OrderStatus.PAID.getCode(), OrderStatus.COMPLETED.getCode())
                .select(Order::getSpotId)
        );

        debugInfo.setBehaviorStats(List.of(
            buildBehaviorStat("浏览", views.stream().map(UserSpotView::getSpotId).collect(Collectors.toList()), "来源表 user_spot_view，统计所有浏览记录"),
            buildBehaviorStat("收藏", favorites.stream().map(UserSpotFavorite::getSpotId).collect(Collectors.toList()), "仅统计 is_deleted = 0 的有效收藏"),
            buildBehaviorStat("评分", reviews.stream().map(Review::getSpotId).collect(Collectors.toList()), "仅统计 is_deleted = 0 的有效评分"),
            buildBehaviorStat("订单", orders.stream().map(Order::getSpotId).collect(Collectors.toList()), "仅统计 PAID 和 COMPLETED 的有效订单"),
            new RecommendationResponse.BehaviorStat("合并后", null, debugInfo.getInteractionCount(), "四类行为按景点合并并加权后，最终进入 r_ui 计算的唯一景点数")
        ));
    }

    /**
     * 展开每条原始行为对应的加权结果，便于定位单个景点分数来源。
     */
    public void populateBehaviorDetails(RecommendationResponse.DebugInfo debugInfo, Long userId, RecommendationAlgorithmConfigDTO config) {
        if (debugInfo == null || userId == null) {
            return;
        }

        List<RecommendationResponse.BehaviorDetail> details = new ArrayList<>();

        userSpotViewMapper.selectList(
            new LambdaQueryWrapper<UserSpotView>()
                .eq(UserSpotView::getUserId, userId)
                .select(UserSpotView::getSpotId, UserSpotView::getViewSource, UserSpotView::getViewDuration)
        ).forEach(view -> details.add(new RecommendationResponse.BehaviorDetail(
            "浏览",
            view.getSpotId(),
            recommendationMetadataSupport.getSpotName(view.getSpotId()),
            recommendationInteractionSupport.calculateViewWeight(view, config),
            String.format(
                Locale.ROOT,
                "来源=%s，停留=%s秒",
                view.getViewSource() == null || view.getViewSource().isBlank() ? "detail" : view.getViewSource(),
                view.getViewDuration() == null ? 0 : view.getViewDuration()
            )
        )));

        userSpotFavoriteMapper.selectList(
            new LambdaQueryWrapper<UserSpotFavorite>()
                .eq(UserSpotFavorite::getUserId, userId)
                .eq(UserSpotFavorite::getIsDeleted, 0)
                .select(UserSpotFavorite::getSpotId)
        ).forEach(favorite -> details.add(new RecommendationResponse.BehaviorDetail(
            "收藏",
            favorite.getSpotId(),
            recommendationMetadataSupport.getSpotName(favorite.getSpotId()),
            defaultDouble(config.getWeightFavorite(), 1.0),
            "有效收藏记录"
        )));

        reviewMapper.selectList(
            new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getIsDeleted, 0)
                .select(Review::getSpotId, Review::getScore)
        ).forEach(review -> details.add(new RecommendationResponse.BehaviorDetail(
            "评分",
            review.getSpotId(),
            recommendationMetadataSupport.getSpotName(review.getSpotId()),
            review.getScore() * defaultDouble(config.getWeightReviewFactor(), 0.4),
            String.format(Locale.ROOT, "评分=%s，因子=%.2f", review.getScore(), defaultDouble(config.getWeightReviewFactor(), 0.4))
        )));

        orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getIsDeleted, 0)
                .in(Order::getStatus, OrderStatus.PAID.getCode(), OrderStatus.COMPLETED.getCode())
                .select(Order::getSpotId, Order::getStatus)
        ).forEach(order -> {
            boolean completed = order.getStatus() == OrderStatus.COMPLETED.getCode();
            details.add(new RecommendationResponse.BehaviorDetail(
                completed ? "订单(已完成)" : "订单(已支付)",
                order.getSpotId(),
                recommendationMetadataSupport.getSpotName(order.getSpotId()),
                completed ? defaultDouble(config.getWeightOrderCompleted(), 4.0) : defaultDouble(config.getWeightOrderPaid(), 3.0),
                completed ? "订单状态=COMPLETED" : "订单状态=PAID"
            ));
        });

        details.sort(Comparator
            .comparing(RecommendationResponse.BehaviorDetail::getScore, Comparator.nullsLast(Double::compareTo))
            .reversed()
            .thenComparing(RecommendationResponse.BehaviorDetail::getSpotId, Comparator.nullsLast(Long::compareTo)));
        debugInfo.setBehaviorDetails(details);
    }

    public void populateScoreDebugEntries(RecommendationResponse.DebugInfo debugInfo, Map<Long, Double> scores, String description) {
        if (debugInfo == null) {
            return;
        }
        debugInfo.setCandidateCount(scores.size());
        debugInfo.setCandidateScores(toDebugEntries(scores, description));
    }

    public void populateFilteredScoresDebugEntries(RecommendationResponse.DebugInfo debugInfo, Map<Long, Double> scores, String description) {
        if (debugInfo == null) {
            return;
        }
        debugInfo.setFilteredCount(scores.size());
        debugInfo.setFilteredScores(toDebugEntries(scores, description));
    }

    public void populateRerankedScoreDebugEntries(RecommendationResponse.DebugInfo debugInfo, Map<Long, Double> scores, String description) {
        if (debugInfo == null) {
            return;
        }
        debugInfo.setRerankedScores(toDebugEntries(scores, description));
    }

    public void populateFilteredOutDebugEntries(RecommendationResponse.DebugInfo debugInfo, List<Long> originalIds, List<Long> filteredIds, String description) {
        if (debugInfo == null) {
            return;
        }
        Set<Long> filteredSet = new HashSet<>(filteredIds);
        List<RecommendationResponse.DebugEntry> removedItems = originalIds.stream()
            .filter(id -> !filteredSet.contains(id))
            .map(id -> new RecommendationResponse.DebugEntry(id, recommendationMetadataSupport.getSpotName(id), null, description))
            .collect(Collectors.toList());
        debugInfo.setFilteredOutItems(removedItems);
    }

    public List<RecommendationResponse.ResultContribution> buildResultContributions(
        Map<Long, Double> finalScores,
        Map<Long, List<RecommendationResponse.DebugEntry>> contributionMap
    ) {
        if (finalScores == null || finalScores.isEmpty() || contributionMap == null || contributionMap.isEmpty()) {
            return Collections.emptyList();
        }
        return finalScores.entrySet().stream()
            .limit(20)
            .map(entry -> new RecommendationResponse.ResultContribution(
                entry.getKey(),
                recommendationMetadataSupport.getSpotName(entry.getKey()),
                entry.getValue(),
                contributionMap.getOrDefault(entry.getKey(), Collections.emptyList()).stream()
                    .sorted(Comparator.comparing(RecommendationResponse.DebugEntry::getScore, Comparator.nullsLast(Double::compareTo)).reversed())
                    .limit(5)
                    .collect(Collectors.toList())
            ))
            .collect(Collectors.toList());
    }

    public void logRecommendationPreview(Long userId, RecommendationResponse response, boolean refresh) {
        if (response == null) {
            log.info("推荐调试预览：用户ID={}，是否刷新={}，结果为空", userId, refresh);
            return;
        }

        String items = Optional.ofNullable(response.getList()).orElseGet(Collections::emptyList).stream()
            .map(item -> String.format(
                "{id=%d,name=%s,score=%s,category=%s,region=%s}",
                item.getId(),
                item.getName(),
                item.getScore() == null ? "null" : String.format("%.4f", item.getScore()),
                item.getCategoryName(),
                item.getRegionName()
            ))
            .collect(Collectors.joining(", "));

        log.info(
            "推荐调试预览结果：用户ID={}，是否刷新={}，推荐类型={}，是否需要偏好引导={}，结果明细=[{}]",
            userId,
            refresh,
            response.getType(),
            response.getNeedPreference(),
            items
        );
    }

    public void logUserInteractionWeights(Long userId, Map<Long, Double> userInteractions, boolean debug) {
        if (!debug) {
            log.info("用户交互权重构建完成：用户ID={}，交互景点数={}", userId, userInteractions.size());
            return;
        }
        log.info(
            "用户交互权重明细：用户ID={}，交互景点数={}，权重详情=[{}]",
            userId,
            userInteractions.size(),
            formatSpotScoreEntries(userInteractions, 20)
        );
    }

    public void logScoreMap(String stage, Map<Long, Double> scoreMap, boolean debug) {
        if (!debug || scoreMap == null) {
            return;
        }
        log.info("{}：候选数={}，明细=[{}]", stage, scoreMap.size(), formatSpotScoreEntries(scoreMap, 20));
    }

    public void logFilteredRecommendations(Long userId, List<Long> originalIds, List<Long> filteredIds, boolean debug) {
        if (!debug) {
            return;
        }
        Set<Long> filteredSet = new LinkedHashSet<>(filteredIds);
        List<Long> removedIds = originalIds.stream().filter(id -> !filteredSet.contains(id)).collect(Collectors.toList());
        log.info(
            "候选过滤结果：用户ID={}，过滤前数量={}，过滤后数量={}，被过滤景点=[{}]",
            userId,
            originalIds.size(),
            filteredIds.size(),
            formatSpotIdList(removedIds, 20)
        );
    }

    public void logColdStartResult(Long userId, String type, List<Long> categoryIds, List<Long> spotIds, boolean debug) {
        String reason = "preference".equals(type) ? "存在用户偏好，按偏好分类推荐" : "缺少足够个性化信号，回退到热门推荐";
        log.info(
            "冷启动推荐结果：用户ID={}，类型={}，原因={}，偏好分类ID={}，候选景点=[{}]",
            userId,
            type,
            reason,
            categoryIds,
            formatSpotIdList(spotIds, debug ? 20 : 10)
        );
    }

    public void logHeatRerankDetails(Map<Long, Double> beforeScores, Map<Long, Double> afterScores, Map<Long, Integer> heatMap, double rerankFactor, int maxHeat) {
        String details = afterScores.entrySet().stream()
            .limit(20)
            .map(entry -> {
                Long spotId = entry.getKey();
                double before = beforeScores.getOrDefault(spotId, 0.0);
                int heat = heatMap.getOrDefault(spotId, 0);
                double after = entry.getValue();
                return String.format(
                    Locale.ROOT,
                    "{景点ID=%d,景点名称=%s,原始分数=%.4f,热度值=%d,热度加成=%.4f,重排后分数=%.4f}",
                    spotId,
                    recommendationMetadataSupport.getSpotName(spotId),
                    before,
                    heat,
                    after - before,
                    after
                );
            })
            .collect(Collectors.joining(", "));
        log.info("热度重排结果：重排系数={}，最大热度={}，重排明细=[{}]", rerankFactor, maxHeat, details);
    }

    public void logUserItemMatrixSamples(Map<Long, Map<Long, Double>> userItemMatrix) {
        String samples = userItemMatrix.entrySet().stream()
            .sorted(Map.Entry.<Long, Map<Long, Double>>comparingByKey())
            .limit(10)
            .map(entry -> String.format(
                Locale.ROOT,
                "{用户ID=%d,交互数=%d,交互权重=[%s]}",
                entry.getKey(),
                entry.getValue().size(),
                formatSpotScoreEntries(entry.getValue(), 10)
            ))
            .collect(Collectors.joining(", "));
        log.info("离线矩阵更新：用户-景点交互矩阵样本=[{}]", samples);
    }

    public void logUserActivitySamples(Map<Long, Integer> userActivityCount) {
        String samples = userActivityCount.entrySet().stream()
            .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
            .limit(10)
            .map(entry -> String.format(Locale.ROOT, "{用户ID=%d,交互景点数=%d}", entry.getKey(), entry.getValue()))
            .collect(Collectors.joining(", "));
        log.info("离线矩阵更新：用户活跃度样本=[{}]", samples);
    }

    public void logSpotSimilaritySummary(Long spotId, Map<Long, Double> topSimilarities) {
        String detail = topSimilarities.entrySet().stream()
            .limit(10)
            .map(entry -> String.format(
                Locale.ROOT,
                "{相似景点ID=%d,相似景点名称=%s,相似度=%.6f}",
                entry.getKey(),
                recommendationMetadataSupport.getSpotName(entry.getKey()),
                entry.getValue()
            ))
            .collect(Collectors.joining(", "));
        log.info(
            "离线矩阵更新：景点ID={}，景点名称={}，Top-K 相似邻居数={}，相似邻居明细=[{}]",
            spotId,
            recommendationMetadataSupport.getSpotName(spotId),
            topSimilarities.size(),
            detail
        );
    }

    private RecommendationResponse.BehaviorStat buildBehaviorStat(String behavior, List<Long> spotIds, String description) {
        Set<Long> uniqueSpotIds = spotIds == null ? Collections.emptySet() : new HashSet<>(spotIds);
        return new RecommendationResponse.BehaviorStat(
            behavior,
            spotIds == null ? 0 : spotIds.size(),
            uniqueSpotIds.size(),
            description
        );
    }

    private List<RecommendationResponse.DebugEntry> toDebugEntries(Map<Long, Double> scoreMap, String description) {
        if (scoreMap == null || scoreMap.isEmpty()) {
            return Collections.emptyList();
        }
        return scoreMap.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(30)
            .map(entry -> new RecommendationResponse.DebugEntry(
                entry.getKey(),
                recommendationMetadataSupport.getSpotName(entry.getKey()),
                entry.getValue(),
                description
            ))
            .collect(Collectors.toList());
    }

    private String formatSpotScoreEntries(Map<Long, Double> scoreMap, int limit) {
        if (scoreMap == null || scoreMap.isEmpty()) {
            return "";
        }
        return scoreMap.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(limit)
            .map(entry -> String.format(
                Locale.ROOT,
                "{景点ID=%d,景点名称=%s,分数=%.4f}",
                entry.getKey(),
                recommendationMetadataSupport.getSpotName(entry.getKey()),
                entry.getValue()
            ))
            .collect(Collectors.joining(", "));
    }

    private String formatSpotIdList(List<Long> spotIds, int limit) {
        if (spotIds == null || spotIds.isEmpty()) {
            return "";
        }
        return spotIds.stream()
            .limit(limit)
            .map(spotId -> String.format(Locale.ROOT, "{景点ID=%d,景点名称=%s}", spotId, recommendationMetadataSupport.getSpotName(spotId)))
            .collect(Collectors.joining(", "));
    }

    private double defaultDouble(Double value, double fallback) {
        return value == null ? fallback : value;
    }
}
