package com.travel.service.support.recommendation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.dto.home.response.HotSpotResponse;
import com.travel.dto.recommendation.config.RecommendationAlgorithmConfigDTO;
import com.travel.dto.recommendation.response.RecommendationResponse;
import com.travel.entity.Spot;
import com.travel.entity.UserPreference;
import com.travel.mapper.SpotMapper;
import com.travel.mapper.UserPreferenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 推荐冷启动支撑，集中处理偏好兜底、热门补齐和用户偏好解析。
 */
@Component
@RequiredArgsConstructor
public class RecommendationColdStartSupport {

    private final SpotMapper spotMapper;
    private final UserPreferenceMapper userPreferenceMapper;

    public RecommendationResponse handleColdStart(
        Long userId,
        Integer limit,
        boolean refresh,
        boolean debug,
        boolean stable,
        RecommendationResponse.DebugInfo debugInfo,
        RecommendationAlgorithmConfigDTO algorithmConfig,
        HotSpotResponse hotSpots,
        java.util.function.IntUnaryOperator coldStartExpandFactorProvider,
        java.util.function.BiFunction<List<Long>, Integer, List<Long>> rotateIds,
        java.util.function.BiConsumer<List<HotSpotResponse.SpotItem>, Integer> rotateHotItems,
        java.util.function.Function<List<Long>, RecommendationResponse> preferenceResponseBuilder,
        java.util.function.Function<List<HotSpotResponse.SpotItem>, RecommendationResponse> hotResponseBuilder,
        java.util.function.Consumer<ColdStartLogContext> coldStartLogger
    ) {
        List<Long> categoryIds = getUserPreferenceCategoryIds(userId);

        if (!categoryIds.isEmpty()) {
            int coldStartLimit = refresh ? Math.max(limit * coldStartExpandFactorProvider.applyAsInt(1), limit) : limit;
            List<Spot> spots = spotMapper.selectList(
                new LambdaQueryWrapper<Spot>()
                    .eq(Spot::getIsPublished, 1)
                    .in(Spot::getCategoryId, categoryIds)
                    .eq(Spot::getIsDeleted, 0)
                    .orderByDesc(Spot::getHeatScore)
                    .last("LIMIT " + coldStartLimit)
            );

            LinkedHashSet<Long> recommendedSpotIds = spots.stream()
                .map(Spot::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
            if (recommendedSpotIds.size() < limit) {
                recommendedSpotIds.addAll(getHotFallbackSpotIds(recommendedSpotIds, coldStartLimit));
            }

            List<Long> spotIds = new ArrayList<>(recommendedSpotIds);
            if (refresh && !stable) {
                spotIds = rotateIds.apply(spotIds, limit);
            }
            if (debugInfo != null) {
                debugInfo.setTriggerReason("交互不足，但存在用户偏好标签");
                debugInfo.setFinalCount(Math.min(spotIds.size(), limit));
                List<String> notes = new ArrayList<>();
                notes.add("当前结果来自偏好冷启动链路。");
                if (spots.size() < limit) {
                    notes.add("偏好分类景点不足，已使用热门景点补齐结果数量。");
                } else {
                    notes.add("请检查用户偏好标签与返回景点分类是否匹配。");
                }
                debugInfo.setNotes(notes);
                debugInfo.setExtra(Map.of("categoryIds", categoryIds));
            }
            coldStartLogger.accept(new ColdStartLogContext(userId, "preference", categoryIds, spotIds, debug));
            return preferenceResponseBuilder.apply(spotIds);
        }

        List<HotSpotResponse.SpotItem> hotSpotList = new ArrayList<>(hotSpots.getList());
        if (refresh && !stable) {
            rotateHotItems.accept(hotSpotList, limit);
        }
        if (debugInfo != null) {
            debugInfo.setTriggerReason("交互不足且没有偏好标签");
            debugInfo.setFinalCount(Math.min(hotSpotList.size(), limit));
            debugInfo.setNotes(List.of(
                "当前结果来自热门兜底链路。",
                "请检查热度分数、上架状态和冷启动策略。"
            ));
        }
        coldStartLogger.accept(new ColdStartLogContext(
            userId,
            "hot",
            Collections.emptyList(),
            hotSpotList.stream().map(HotSpotResponse.SpotItem::getId).collect(Collectors.toList()),
            debug
        ));
        return hotResponseBuilder.apply(hotSpotList);
    }

    public List<Long> getHotFallbackSpotIds(Collection<Long> excludedSpotIds, int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<Spot> queryWrapper = new LambdaQueryWrapper<Spot>()
            .eq(Spot::getIsPublished, 1)
            .eq(Spot::getIsDeleted, 0);
        if (excludedSpotIds != null && !excludedSpotIds.isEmpty()) {
            queryWrapper.notIn(Spot::getId, excludedSpotIds);
        }
        queryWrapper.orderByDesc(Spot::getHeatScore).last("LIMIT " + limit);

        return spotMapper.selectList(queryWrapper).stream().map(Spot::getId).collect(Collectors.toList());
    }

    public List<Long> getUserPreferenceCategoryIds(Long userId) {
        List<UserPreference> preferences = userPreferenceMapper.selectList(
            new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getUserId, userId)
                .eq(UserPreference::getIsDeleted, 0)
        );
        if (preferences.isEmpty()) {
            return Collections.emptyList();
        }

        return preferences.stream()
            .map(UserPreference::getTag)
            .map(this::parsePreferenceCategoryId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }

    public Long parsePreferenceCategoryId(String tag) {
        if (tag == null || tag.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(tag.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public record ColdStartLogContext(Long userId, String type, List<Long> categoryIds, List<Long> spotIds, boolean debug) {
    }
}
