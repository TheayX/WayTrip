package com.travel.service.support.recommendation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.dto.recommendation.config.RecommendationAlgorithmConfigDTO;
import com.travel.dto.recommendation.config.RecommendationHeatConfigDTO;
import com.travel.dto.recommendation.response.RecommendationResponse;
import com.travel.entity.Order;
import com.travel.entity.Review;
import com.travel.entity.Spot;
import com.travel.entity.UserSpotFavorite;
import com.travel.entity.UserSpotView;
import com.travel.enums.OrderStatus;
import com.travel.mapper.OrderMapper;
import com.travel.mapper.ReviewMapper;
import com.travel.mapper.SpotMapper;
import com.travel.mapper.UserSpotFavoriteMapper;
import com.travel.mapper.UserSpotViewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 推荐分数支撑，集中处理用户行为权重、候选过滤重排与调试信息输出。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationScoreSupport {

    private final SpotMapper spotMapper;
    private final UserSpotViewMapper userSpotViewMapper;
    private final UserSpotFavoriteMapper userSpotFavoriteMapper;
    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final RecommendationQuerySupport recommendationQuerySupport;
    private final RecommendationViewSourceClassifier recommendationViewSourceClassifier;

    public Map<Long, Double> buildUserInteractionWeights(Long userId, RecommendationAlgorithmConfigDTO config) {
        Map<Long, Double> weights = new HashMap<>();
        Map<Long, Double> viewWeights = new HashMap<>();
        Map<Long, Double> favoriteWeights = new HashMap<>();
        Map<Long, Double> reviewWeights = new HashMap<>();
        Map<Long, Double> orderWeights = new HashMap<>();

        userSpotViewMapper.selectList(
            new LambdaQueryWrapper<UserSpotView>()
                .eq(UserSpotView::getUserId, userId)
                .select(UserSpotView::getSpotId, UserSpotView::getViewSource, UserSpotView::getViewDuration)
        ).forEach(view -> mergeBehaviorWeight(viewWeights, view.getSpotId(), calculateViewWeight(view, config)));

        userSpotFavoriteMapper.selectList(
            new LambdaQueryWrapper<UserSpotFavorite>()
                .eq(UserSpotFavorite::getUserId, userId)
                .eq(UserSpotFavorite::getIsDeleted, 0)
                .select(UserSpotFavorite::getSpotId)
        ).forEach(favorite -> mergeBehaviorWeight(favoriteWeights, favorite.getSpotId(), config.getWeightFavorite()));

        reviewMapper.selectList(
            new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getIsDeleted, 0)
                .select(Review::getSpotId, Review::getScore)
        ).forEach(review -> mergeBehaviorWeight(
            reviewWeights,
            review.getSpotId(),
            review.getScore() * config.getWeightReviewFactor()
        ));

        orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getIsDeleted, 0)
                .in(Order::getStatus, OrderStatus.PAID.getCode(), OrderStatus.COMPLETED.getCode())
                .select(Order::getSpotId, Order::getStatus)
        ).forEach(order -> {
            double weight = order.getStatus() == OrderStatus.COMPLETED.getCode()
                ? config.getWeightOrderCompleted()
                : config.getWeightOrderPaid();
            mergeBehaviorWeight(orderWeights, order.getSpotId(), weight);
        });

        mergeInteractionWeight(weights, viewWeights);
        mergeInteractionWeight(weights, favoriteWeights);
        mergeInteractionWeight(weights, reviewWeights);
        mergeInteractionWeight(weights, orderWeights);
        return weights;
    }

    public double calculateViewWeight(UserSpotView view, RecommendationAlgorithmConfigDTO config) {
        double baseWeight = config.getWeightView() == null ? 0.5 : config.getWeightView();
        return baseWeight
            * getViewSourceFactor(view.getViewSource(), config)
            * getViewDurationFactor(view.getViewDuration(), config);
    }

    public void mergeInteractionWeight(Map<Long, Double> weights, Long spotId, Double weight) {
        if (weights == null || spotId == null || weight == null || weight <= 0) {
            return;
        }
        weights.merge(spotId, weight, Double::sum);
    }

    public void mergeBehaviorWeight(Map<Long, Double> weights, Long spotId, Double weight) {
        if (weights == null || spotId == null || weight == null || weight <= 0) {
            return;
        }
        weights.merge(spotId, weight, Math::max);
    }

    public void mergeInteractionWeight(Map<Long, Double> targetWeights, Map<Long, Double> behaviorWeights) {
        if (targetWeights == null || behaviorWeights == null || behaviorWeights.isEmpty()) {
            return;
        }
        behaviorWeights.forEach((spotId, weight) -> mergeInteractionWeight(targetWeights, spotId, weight));
    }

    public Map<Long, Double> applyHeatRerank(Map<Long, Double> scoreMap, RecommendationHeatConfigDTO config, boolean debug) {
        if (scoreMap == null || scoreMap.isEmpty()) {
            return Collections.emptyMap();
        }

        double rerankFactor = config.getHeatRerankFactor() == null ? 0.0 : config.getHeatRerankFactor();
        if (rerankFactor <= 0) {
            if (debug) {
                log.info("跳过热度重排：原因=热度重排系数小于等于0，当前系数={}", rerankFactor);
            }
            return new LinkedHashMap<>(scoreMap);
        }

        List<Spot> spots = spotMapper.selectBatchIds(scoreMap.keySet());
        Map<Long, Integer> heatMap = spots.stream()
            .filter(spot -> spot.getIsDeleted() == 0 && spot.getIsPublished() == 1)
            .collect(Collectors.toMap(Spot::getId, spot -> Optional.ofNullable(spot.getHeatScore()).orElse(0)));

        int maxHeat = heatMap.values().stream().max(Integer::compareTo).orElse(0);
        if (maxHeat <= 0) {
            if (debug) {
                log.info("跳过热度重排：原因=候选景点热度均为空或为0");
            }
            return new LinkedHashMap<>(scoreMap);
        }

        Map<Long, Double> rerankedScores = scoreMap.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue() + rerankFactor * (heatMap.getOrDefault(entry.getKey(), 0) / (double) maxHeat),
                (left, right) -> left,
                LinkedHashMap::new
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (left, right) -> left,
                LinkedHashMap::new
            ));

        if (debug) {
            logHeatRerankDetails(scoreMap, rerankedScores, heatMap, rerankFactor, maxHeat);
        }
        return rerankedScores;
    }

    public List<Long> filterInteractedSpots(Long userId, List<Long> spotIds) {
        if (spotIds == null || spotIds.isEmpty()) {
            return spotIds;
        }

        Set<Long> ratedIds = reviewMapper.selectList(
            new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getIsDeleted, 0)
        ).stream().map(Review::getSpotId).collect(Collectors.toSet());

        Set<Long> favoriteIds = userSpotFavoriteMapper.selectList(
            new LambdaQueryWrapper<UserSpotFavorite>()
                .eq(UserSpotFavorite::getUserId, userId)
                .eq(UserSpotFavorite::getIsDeleted, 0)
        ).stream().map(UserSpotFavorite::getSpotId).collect(Collectors.toSet());

        Set<Long> orderedIds = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getIsDeleted, 0)
                .ne(Order::getStatus, OrderStatus.CANCELLED.getCode())
        ).stream().map(Order::getSpotId).collect(Collectors.toSet());

        Set<Long> excludeIds = new HashSet<>();
        excludeIds.addAll(ratedIds);
        excludeIds.addAll(favoriteIds);
        excludeIds.addAll(orderedIds);

        return spotIds.stream()
            .filter(id -> !excludeIds.contains(id))
            .collect(Collectors.toList());
    }

    public Map<Long, Double> orderScoresByIds(List<Long> orderedIds, Map<Long, Double> scoreMap) {
        if (orderedIds == null || orderedIds.isEmpty() || scoreMap == null || scoreMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Double> orderedScores = new LinkedHashMap<>();
        for (Long spotId : orderedIds) {
            Double score = scoreMap.get(spotId);
            if (score != null) {
                orderedScores.put(spotId, score);
            }
        }
        return orderedScores;
    }

    public Map<Long, Double> castScoreMap(Map<?, ?> rawMap) {
        Map<Long, Double> scoreMap = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            Long spotId = castToLong(entry.getKey());
            Double score = castToDouble(entry.getValue());
            if (spotId != null && score != null) {
                scoreMap.put(spotId, score);
            }
        }
        return scoreMap;
    }

    public Long castToLong(Object value) {
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Integer intValue) {
            return intValue.longValue();
        }
        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException e) {
                log.warn("Redis Key 转 Long 失败：{}", stringValue);
            }
        }
        return null;
    }

    public Double castToDouble(Object value) {
        if (value instanceof Double doubleValue) {
            return doubleValue;
        }
        if (value instanceof Float floatValue) {
            return floatValue.doubleValue();
        }
        if (value instanceof Number numberValue) {
            return numberValue.doubleValue();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return Double.parseDouble(stringValue);
            } catch (NumberFormatException e) {
                log.warn("Redis 值转 Double 失败：{}", stringValue);
            }
        }
        return null;
    }

    public RecommendationResponse.DebugInfo initDebugInfo(Long userId, Integer limit, boolean refresh) {
        RecommendationResponse.DebugInfo debugInfo = new RecommendationResponse.DebugInfo();
        debugInfo.setUserId(userId);
        debugInfo.setRequestLimit(limit);
        debugInfo.setRefresh(refresh);
        debugInfo.setDebugEnabled(true);
        return debugInfo;
    }

    public void populateInteractionDebugInfo(RecommendationResponse.DebugInfo debugInfo, Map<Long, Double> userInteractions) {
        if (debugInfo == null) {
            return;
        }
        debugInfo.setInteractionCount(userInteractions.size());
        debugInfo.setUserInteractions(toDebugEntries(userInteractions, "用户对该景点的融合交互权重"));
    }

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
            recommendationQuerySupport.getSpotName(view.getSpotId()),
            calculateViewWeight(view, config),
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
            recommendationQuerySupport.getSpotName(favorite.getSpotId()),
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
            recommendationQuerySupport.getSpotName(review.getSpotId()),
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
                recommendationQuerySupport.getSpotName(order.getSpotId()),
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

    public void populateInteractionDebugInfoDetailed(RecommendationResponse.DebugInfo debugInfo, Map<Long, Double> userInteractions) {
        if (debugInfo == null) {
            return;
        }
        debugInfo.setInteractionCount(userInteractions.size());
        Map<Long, String> interactionDescriptions = buildInteractionDescriptions(debugInfo.getBehaviorDetails());
        debugInfo.setUserInteractions(userInteractions.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .map(entry -> new RecommendationResponse.DebugEntry(
                entry.getKey(),
                recommendationQuerySupport.getSpotName(entry.getKey()),
                entry.getValue(),
                interactionDescriptions.getOrDefault(entry.getKey(), "用户对该景点的融合交互权重")
            ))
            .collect(Collectors.toList()));
    }

    private Map<Long, String> buildInteractionDescriptions(List<RecommendationResponse.BehaviorDetail> behaviorDetails) {
        if (behaviorDetails == null || behaviorDetails.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Map<String, RecommendationResponse.BehaviorDetail>> groupedDetails = new LinkedHashMap<>();
        for (RecommendationResponse.BehaviorDetail detail : behaviorDetails) {
            if (detail == null || detail.getSpotId() == null || detail.getBehavior() == null) {
                continue;
            }

            groupedDetails
                .computeIfAbsent(detail.getSpotId(), key -> new LinkedHashMap<>())
                .merge(detail.getBehavior(), detail, (left, right) -> {
                    double leftScore = left.getScore() == null ? Double.NEGATIVE_INFINITY : left.getScore();
                    double rightScore = right.getScore() == null ? Double.NEGATIVE_INFINITY : right.getScore();
                    return rightScore > leftScore ? right : left;
                });
        }

        Map<Long, String> descriptions = new HashMap<>();
        for (Map.Entry<Long, Map<String, RecommendationResponse.BehaviorDetail>> entry : groupedDetails.entrySet()) {
            List<RecommendationResponse.BehaviorDetail> details = new ArrayList<>(entry.getValue().values());
            details.sort(
                Comparator.comparingInt((RecommendationResponse.BehaviorDetail detail) -> getBehaviorPriority(detail.getBehavior()))
                    .thenComparing(
                        RecommendationResponse.BehaviorDetail::getScore,
                        Comparator.nullsLast(Comparator.reverseOrder())
                    )
            );

            String mergedDescription = details.stream()
                .map(detail -> String.format(Locale.ROOT, "%s%.4f", detail.getBehavior(), defaultDouble(detail.getScore(), 0.0)))
                .collect(Collectors.joining("+"));

            descriptions.put(entry.getKey(), "用户对该景点的融合交互权重：" + mergedDescription);
        }
        return descriptions;
    }

    private int getBehaviorPriority(String behavior) {
        if (behavior == null) {
            return Integer.MAX_VALUE;
        }
        if (behavior.startsWith("订单")) {
            return 0;
        }
        if ("评分".equals(behavior)) {
            return 1;
        }
        if ("收藏".equals(behavior)) {
            return 2;
        }
        if ("浏览".equals(behavior)) {
            return 3;
        }
        return 4;
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
            .map(id -> new RecommendationResponse.DebugEntry(id, recommendationQuerySupport.getSpotName(id), null, description))
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
                recommendationQuerySupport.getSpotName(entry.getKey()),
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
                    recommendationQuerySupport.getSpotName(spotId),
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
                recommendationQuerySupport.getSpotName(entry.getKey()),
                entry.getValue()
            ))
            .collect(Collectors.joining(", "));
        log.info(
            "离线矩阵更新：景点ID={}，景点名称={}，Top-K 相似邻居数={}，相似邻居明细=[{}]",
            spotId,
            recommendationQuerySupport.getSpotName(spotId),
            topSimilarities.size(),
            detail
        );
    }

    private double getViewSourceFactor(String source, RecommendationAlgorithmConfigDTO config) {
        return switch (recommendationViewSourceClassifier.normalize(source)) {
            case "search" -> defaultDouble(config.getViewSourceFactorSearch(), 1.2);
            case "recommendation" -> defaultDouble(config.getViewSourceFactorRecommendation(), 1.1);
            case "home" -> defaultDouble(config.getViewSourceFactorHome(), 0.9);
            case "guide" -> defaultDouble(config.getViewSourceFactorGuide(), 1.0);
            default -> defaultDouble(config.getViewSourceFactorDetail(), 1.0);
        };
    }

    private double getViewDurationFactor(Integer duration, RecommendationAlgorithmConfigDTO config) {
        int seconds = duration == null ? 0 : Math.max(duration, 0);
        int shortThreshold = defaultInt(config.getViewDurationShortThresholdSeconds(), 10);
        int mediumThreshold = Math.max(shortThreshold, defaultInt(config.getViewDurationMediumThresholdSeconds(), 60));
        int longThreshold = Math.max(mediumThreshold, defaultInt(config.getViewDurationLongThresholdSeconds(), 180));

        if (seconds < shortThreshold) {
            return defaultDouble(config.getViewDurationFactorShort(), 0.6);
        }
        if (seconds < mediumThreshold) {
            return defaultDouble(config.getViewDurationFactorMedium(), 1.0);
        }
        if (seconds < longThreshold) {
            return defaultDouble(config.getViewDurationFactorLong(), 1.2);
        }
        return defaultDouble(config.getViewDurationFactorVeryLong(), 1.35);
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
                recommendationQuerySupport.getSpotName(entry.getKey()),
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
                recommendationQuerySupport.getSpotName(entry.getKey()),
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
            .map(spotId -> String.format(Locale.ROOT, "{景点ID=%d,景点名称=%s}", spotId, recommendationQuerySupport.getSpotName(spotId)))
            .collect(Collectors.joining(", "));
    }

    private double defaultDouble(Double value, double fallback) {
        return value == null ? fallback : value;
    }

    private int defaultInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }
}
