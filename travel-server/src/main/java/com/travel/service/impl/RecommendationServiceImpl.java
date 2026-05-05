package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.constant.ResourceDisplayText;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.dto.home.response.HotSpotResponse;
import com.travel.dto.home.response.NearbySpotResponse;
import com.travel.dto.home.item.RecentViewedSpotItem;
import com.travel.dto.home.response.RecentViewedSpotResponse;
import com.travel.dto.recommendation.cache.UserRecommendationCacheDTO;
import com.travel.dto.recommendation.config.RecommendationAlgorithmConfigDTO;
import com.travel.dto.recommendation.config.RecommendationCacheConfigDTO;
import com.travel.dto.recommendation.config.RecommendationConfigBundleDTO;
import com.travel.dto.recommendation.config.RecommendationHeatConfigDTO;
import com.travel.dto.recommendation.response.RecommendationResponse;
import com.travel.dto.recommendation.response.RecommendationStatusDTO;
import com.travel.dto.recommendation.response.SimilarityPreviewResponse;
import com.travel.entity.*;
import com.travel.mapper.*;
import com.travel.service.cache.RecommendationCacheService;
import com.travel.service.RecommendationService;
import com.travel.service.support.recommendation.RecommendationConfigSupport;
import com.travel.service.support.recommendation.RecommendationColdStartSupport;
import com.travel.service.support.recommendation.RecommendationQuerySupport;
import com.travel.service.support.recommendation.RecommendationScoreSupport;
import com.travel.service.support.recommendation.RecommendationSimilaritySupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 推荐服务实现，负责个性化推荐、冷启动兜底、相似度矩阵维护与调试能力。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private static final String PREVIEW_MODE_CACHE = "cache";
    private static final String PREVIEW_MODE_RECOMPUTE = "recompute";
    private static final String PREVIEW_MODE_RECOMPUTE_ROTATE = "recompute_rotate";

    // 持久层、缓存与推荐支持组件

    private final SpotMapper spotMapper;
    private final ReviewMapper reviewMapper;
    private final UserSpotFavoriteMapper userSpotFavoriteMapper;
    private final OrderMapper orderMapper;
    private final UserSpotViewMapper userSpotViewMapper;
    private final SpotCategoryMapper categoryMapper;
    private final SpotRegionMapper spotRegionMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final UserMapper userMapper;
    private final RecommendationCacheService recommendationCacheService;
    private final RecommendationQuerySupport recommendationQuerySupport;
    private final RecommendationConfigSupport recommendationConfigSupport;
    private final RecommendationSimilaritySupport recommendationSimilaritySupport;
    private final RecommendationScoreSupport recommendationScoreSupport;
    private final RecommendationColdStartSupport recommendationColdStartSupport;

    private final AtomicBoolean computing = new AtomicBoolean(false);
    // 推荐主链路入口

    @Override
    public RecommendationResponse getRecommendations(Long userId, Integer limit) {
        if (limit == null || limit <= 0) limit = 10;

        // 优先复用缓存，避免每次都触发协同过滤计算和多表查询。
        UserRecommendationCacheDTO cached = recommendationCacheService.getUserRecommendation(userId);
        if (cached != null) {
            RecommendationResponse cachedResponse = buildRecommendationResponseFromCache(cached, limit);
            if (cachedResponse != null) {
                return cachedResponse;
            }
        }

        return computeRecommendations(userId, limit, false, false);
    }

    @Override
    public RecommendationResponse rotateRecommendations(Long userId, Integer limit) {
        if (limit == null || limit <= 0) limit = 10;

        // “换一批”只在当前推荐基线上轮换顺序；缓存缺失时先建立基线再轮换。
        RecommendationResponse response = getRecommendations(userId, limit);
        if (response == null || response.getList() == null || response.getList().size() <= 1) {
            return response;
        }

        response.setList(rotateRecommendationItems(response.getList(), limit));
        saveRecommendationCache(userId, response);
        return response;
    }

    @Override
    public RecommendationResponse recomputeRecommendations(Long userId, Integer limit) {
        if (limit == null || limit <= 0) limit = 10;
        return computeRecommendations(userId, limit, true, false);
    }

    @Override
    public RecommendationResponse previewRecommendations(Long userId, Integer limit, String mode, Boolean debug) {
        if (limit == null || limit <= 0) limit = 10;
        String previewMode = normalizePreviewMode(mode);
        boolean debugMode = Boolean.TRUE.equals(debug);
        RecommendationResponse response = switch (previewMode) {
            case PREVIEW_MODE_RECOMPUTE -> computeRecommendations(userId, limit, true, debugMode, false);
            case PREVIEW_MODE_RECOMPUTE_ROTATE -> computeRecommendations(userId, limit, true, debugMode, true);
            default -> previewCachedRecommendations(userId, limit, debugMode);
        };
        if (debugMode) {
            logRecommendationPreview(userId, response, previewMode);
        }
        return response;
    }

    @Override
    public void invalidateUserRecommendationCache(Long userId) {
        if (userId == null) {
            return;
        }
        // 单用户行为变化后只清理该用户缓存，避免不必要地影响全局命中率。
        recommendationCacheService.deleteUserRecommendation(userId);
    }

    @Override
    public void invalidateGlobalRecommendationCaches() {
        // 热门结果和个性化结果都依赖景点热度、配置或相似度矩阵，因此统一失效。
        recommendationCacheService.deleteHomeHotSpots();
        recommendationCacheService.deleteAllUserRecommendations();
    }

    // 推荐结果计算与冷启动兜底

    /**
     * 默认计算入口，适用于普通“获取推荐”场景。
     */
    private RecommendationResponse computeRecommendations(Long userId, Integer limit) {
        return computeRecommendations(userId, limit, false, false, false);
    }

    /**
     * 仅暴露重算开关，供强制重算场景复用统一主流程。
     */
    private RecommendationResponse computeRecommendations(Long userId, Integer limit, boolean recompute) {
        return computeRecommendations(userId, limit, recompute, false, false);
    }

    /**
     * 兼容调试开关的中间重载，最终都收敛到完整参数版本。
     */
    private RecommendationResponse computeRecommendations(Long userId, Integer limit, boolean recompute, boolean debug) {
        return computeRecommendations(userId, limit, recompute, debug, false);
    }

    /**
     * 推荐主流程。
     * <p>
     * 统一处理配置读取、协同过滤、冷启动降级、热度重排、缓存写入和调试信息组装。
     */
    private RecommendationResponse computeRecommendations(Long userId, Integer limit, boolean recompute, boolean debug, boolean rotateAfterCompute) {
        // 第 1 步：读取配置快照。
        // 所有后续计算都基于同一份配置，避免一次请求内前后参数不一致。
        RecommendationConfigBundleDTO config = recommendationCacheService.loadConfig();
        RecommendationAlgorithmConfigDTO algorithmConfig = safeAlgorithmConfig(config);
        RecommendationHeatConfigDTO heatConfig = safeHeatConfig(config);
        RecommendationCacheConfigDTO cacheConfig = safeCacheConfig(config);
        // 调试信息仅在显式开启时初始化，避免普通请求携带额外组装开销。
        RecommendationResponse.DebugInfo debugInfo = debug ? initDebugInfo(userId, limit, normalizeDebugMode(recompute, rotateAfterCompute)) : null;

        log.info(
            "开始计算推荐结果：用户ID={}，请求数量={}，是否强制重算={}，是否调试={}，协同过滤最少交互数={}，候选扩容倍数={}",
            userId,
            limit,
            recompute,
            debug,
            defaultInt(algorithmConfig.getMinInteractionsForCF(), 3),
            getCandidateExpandFactor(algorithmConfig)
        );

        // 第 2 步：构建用户兴趣输入。
        // 这里拿到的是“用户对哪些景点有多强兴趣”的压缩表达，是整个个性化推荐的输入基线。
        Map<Long, Double> userInteractions = buildUserInteractionWeights(userId, algorithmConfig);
        // 调试信息与日志都围绕这份输入展开，便于先确认“输入是否合理”，再看输出结果。
        populateBehaviorStats(debugInfo, userId);
        populateBehaviorDetails(debugInfo, userId, algorithmConfig);
        populateInteractionDebugInfo(debugInfo, userInteractions);
        logUserInteractionWeights(userId, userInteractions, debug);

        // 第 3 步：根据输入规模决定是否降级。
        // 当用户有效交互过少时，协同过滤缺少可靠的相似性依据，继续强算只会放大噪音。
        if (userInteractions.size() < defaultInt(algorithmConfig.getMinInteractionsForCF(), 3)) {
            log.info(
                "降级为冷启动：用户ID={}，交互景点数={}，阈值={}，原因=交互不足",
                userId,
                userInteractions.size(),
                defaultInt(algorithmConfig.getMinInteractionsForCF(), 3)
            );
            if (debugInfo != null) {
                debugInfo.setTriggerReason("交互数量不足，切换为冷启动");
                debugInfo.setNotes(List.of(
                    "用户交互景点数低于协同过滤阈值。",
                    "请检查用户历史行为是否足够，或适当调低 minInteractionsForCF。"
                ));
            }
            return handleColdStart(userId, limit, rotateAfterCompute, debug, debugInfo);
        }

        // 第 4 步：进入 ItemCF 主链路。
        // 先适度放大候选集，给后续“过滤已交互景点”和“热度重排”预留足够空间。
        int candidateLimit = limit * getCandidateExpandFactor(algorithmConfig);
        Map<Long, List<RecommendationResponse.DebugEntry>> contributionMap = debug ? new HashMap<>() : null;
        Map<Long, Double> recommendedScores = computeItemCFRecommendationScores(userInteractions, candidateLimit, contributionMap);
        // 这一步拿到的是协同过滤原始分数，尚未扣除重复内容，也未引入热度修正。
        populateScoreDebugEntries(debugInfo, recommendedScores, "ItemCF 原始候选分数");
        logScoreMap("ItemCF 原始候选分数", recommendedScores, debug);
        List<Long> recommendedIds = new ArrayList<>(recommendedScores.keySet());
        
        // 第 5 步：过滤已消费内容。
        // 过滤后保留下来的候选，才是真正有资格进入最终推荐列表的“新增内容”。
        List<Long> filteredIds = filterInteractedSpots(userId, recommendedIds);
        populateFilteredOutDebugEntries(debugInfo, recommendedIds, filteredIds, "候选景点已与当前用户发生过交互，已被过滤");
        logFilteredRecommendations(userId, recommendedIds, filteredIds, debug);
        Map<Long, Double> filteredScores = orderScoresByIds(filteredIds, recommendedScores);
        populateFilteredScoresDebugEntries(debugInfo, filteredScores, "过滤后的候选分数");
        logScoreMap("过滤后的候选分数", filteredScores, debug);
        // 第 6 步：执行热度重排。
        // 这里只做“微调排序”，不是替换个性化排序，目标是在相关性与平台热度之间取得平衡。
        filteredScores = applyHeatRerank(filteredScores, heatConfig, debug);
        populateRerankedScoreDebugEntries(debugInfo, filteredScores, "热度重排后的候选分数");
        filteredIds = new ArrayList<>(filteredScores.keySet());
        
        // 经过过滤和重排后如果已经没有可用候选，说明这条个性化链路无法产出可展示结果。
        if (filteredIds.isEmpty()) {
            log.info("个性化推荐结果过滤后为空，降级为冷启动：用户ID={}", userId);
            if (debugInfo != null) {
                debugInfo.setTriggerReason("协同过滤候选集在过滤后为空，降级为冷启动");
            }
            return handleColdStart(userId, limit, rotateAfterCompute, debug, debugInfo);
        }

        // 第 7 步：在明确需要模拟“换一批”时做轻量轮换。
        if (rotateAfterCompute) {
            filteredIds = rotateRecommendations(filteredIds, limit);
            filteredScores = orderScoresByIds(filteredIds, filteredScores);
        }

        log.info(
            "推荐结果计算完成：用户ID={}，推荐类型=personalized，候选数={}，最终返回数={}，缓存时长={}分钟",
            userId,
            recommendedScores.size(),
            Math.min(filteredIds.size(), limit),
            defaultInt(cacheConfig.getUserRecTTLMinutes(), 60)
        );

        // 第 9 步：补齐调试信息并组装响应。
        // 调试模式下会附带触发原因、贡献来源和阶段性说明，便于后台做链路分析。
        if (debugInfo != null) {
            debugInfo.setTriggerReason("命中协同过滤主链路");
            debugInfo.setResultContributions(buildResultContributions(filteredScores, contributionMap));
            debugInfo.setNotes(List.of(
                "当前结果来自个性化协同过滤链路。",
                "可重点关注交互权重、候选分数、结果贡献来源和热度重排变化。"
            ));
        }
        RecommendationResponse response = buildRecommendationResponse(filteredIds, filteredScores, limit, "personalized", false, debugInfo);
        saveRecommendationCache(userId, response, cacheConfig);
        return response;
    }

    // 用户行为权重构建

    /**
     * 构建单个用户的融合交互权重：Map<spotId, weight>。
     * <p>
     * 权重融合了浏览、收藏、评分、订单等行为，作为协同过滤阶段的用户兴趣输入。
     */
    private Map<Long, Double> buildUserInteractionWeights(Long userId, RecommendationAlgorithmConfigDTO config) {
        return recommendationScoreSupport.buildUserInteractionWeights(userId, config);
    }


    // 冷启动推荐与结果轮换

    /**
     * 当协同过滤不可用或结果不足时，统一走冷启动兜底逻辑。
     * <p>
     * 支持按用户偏好推荐；如果偏好也不可用，则继续降级到热门景点。
     */
    private RecommendationResponse handleColdStart(Long userId, Integer limit, boolean rotateAfterCompute, boolean debug,
                                                   RecommendationResponse.DebugInfo debugInfo) {
        RecommendationAlgorithmConfigDTO algorithmConfig = safeAlgorithmConfig(recommendationCacheService.loadConfig());
        // 第 1 步：准备冷启动候选池。
        // 无论最终走偏好推荐还是热门兜底，热门池都会作为基础候选或补齐来源。
        HotSpotResponse hotSpots = getHotSpots(rotateAfterCompute ? Math.max(limit * getColdStartExpandFactor(algorithmConfig), limit) : limit);
        RecommendationResponse response = recommendationColdStartSupport.handleColdStart(
            userId,
            limit,
            rotateAfterCompute,
            debug,
            false,
            debugInfo,
            algorithmConfig,
            hotSpots,
            ignored -> getColdStartExpandFactor(algorithmConfig),
            this::rotateRecommendations,
            this::rotateSpotItems,
            // 偏好链路仍返回标准推荐结构，确保前端无需区分“个性化”与“偏好冷启动”的数据格式。
            spotIds -> buildRecommendationResponse(spotIds, limit, "preference", false, debugInfo),
            hotSpotList -> {
                RecommendationResponse hotResponse = new RecommendationResponse();
                hotResponse.setType("hot");
                hotResponse.setNeedPreference(true);
                hotResponse.setDebugInfo(debugInfo);
                // 热门兜底阶段只关心基础展示字段，直接复用热门卡片结构可以减少重复查询。
                hotResponse.setList(hotSpotList.stream()
                    .limit(limit)
                    .map(item -> {
                        RecommendationResponse.SpotItem spotItem = new RecommendationResponse.SpotItem();
                        spotItem.setId(item.getId());
                        spotItem.setName(item.getName());
                        spotItem.setCoverImage(item.getCoverImage());
                        spotItem.setPrice(item.getPrice());
                        spotItem.setAvgRating(item.getAvgRating());
                        spotItem.setCategoryName(item.getCategoryName());
                        return spotItem;
                    })
                    .collect(Collectors.toList()));
                return hotResponse;
            },
            context -> logColdStartResult(context.userId(), context.type(), context.categoryIds(), context.spotIds(), context.debug())
        );
        saveRecommendationCache(userId, response);
        return response;
    }

    /**
     * 从热门池里补齐候选景点，避免偏好推荐数量不足时直接返回空结果。
     */
    private List<Long> getHotFallbackSpotIds(Collection<Long> excludedSpotIds, int limit) {
        return recommendationColdStartSupport.getHotFallbackSpotIds(excludedSpotIds, limit);
    }

    /**
     * 读取用户偏好分类，为冷启动偏好推荐提供直接输入。
     */
    private List<Long> getUserPreferenceCategoryIds(Long userId) {
        return recommendationColdStartSupport.getUserPreferenceCategoryIds(userId);
    }

    /**
     * 将偏好标签解析成分类 ID，统一收口标签到业务主键的转换逻辑。
     */
    private Long parsePreferenceCategoryId(String tag) {
        return recommendationColdStartSupport.parsePreferenceCategoryId(tag);
    }

    /**
     * 轮换推荐结果，避免每次都看到完全相同的一组。
     * <p>
     * 这里只做有限位移，不打乱整体相关性顺序，避免用户感知到推荐质量明显波动。
     */
    private List<Long> rotateRecommendations(List<Long> recommendationIds, Integer limit) {
        if (recommendationIds == null || recommendationIds.size() <= 1) {
            return recommendationIds;
        }

        // 轮换基于当前结果副本完成，避免调用方感知到原列表被原地修改。
        List<Long> rotatedIds = new ArrayList<>(recommendationIds);
        // 轮换步长不会超过本次真正可展示的范围，避免把尾部低优先级候选大幅提到前面。
        int rotationBase = Math.min(limit, rotatedIds.size() - 1);
        if (rotationBase <= 0) {
            return rotatedIds;
        }

        // 偏移量带一点轻微随机性，用来制造“刷新后有变化”的感知，但变化仍然受控。
        int offset = 1 + Math.abs(Objects.hash(System.nanoTime(), recommendationIds.get(0))) % rotationBase;
        Collections.rotate(rotatedIds, -offset);
        return rotatedIds;
    }

    /**
     * 轮换完整推荐结果项列表，保持“换一批”只调整展示顺序，不改动结果内容。
     */
    private List<RecommendationResponse.SpotItem> rotateRecommendationItems(List<RecommendationResponse.SpotItem> spotItems, Integer limit) {
        if (spotItems == null || spotItems.size() <= 1) {
            return spotItems;
        }

        List<RecommendationResponse.SpotItem> rotatedItems = new ArrayList<>(spotItems);
        int rotationBase = Math.min(limit, rotatedItems.size() - 1);
        if (rotationBase <= 0) {
            return rotatedItems;
        }

        int offset = 1 + Math.abs(Objects.hash(System.nanoTime(), rotatedItems.get(0).getId())) % rotationBase;
        Collections.rotate(rotatedItems, -offset);
        return rotatedItems;
    }

    /**
     * 轮换热门/偏好冷启动结果列表。
     * <p>
     * 与个性化推荐保持同样的轮换策略，确保首页刷新后结果有变化但不会完全失序。
     */
    private void rotateSpotItems(List<HotSpotResponse.SpotItem> spotItems, Integer limit) {
        if (spotItems == null || spotItems.size() <= 1) {
            return;
        }

        // 冷启动轮换与个性化轮换保持同一套边界，保证不同来源的推荐体验一致。
        int rotationBase = Math.min(limit, spotItems.size() - 1);
        if (rotationBase <= 0) {
            return;
        }

        // 以首项和时间作为扰动源，避免多次刷新都呈现完全同一顺序。
        int offset = 1 + Math.abs(Objects.hash(System.nanoTime(), spotItems.get(0).getId())) % rotationBase;
        Collections.rotate(spotItems, -offset);
    }

    /**
     * 将推荐结果写入缓存，供用户端直读与轮换逻辑复用。
     */
    private void saveRecommendationCache(Long userId, RecommendationResponse response) {
        saveRecommendationCache(userId, response, safeCacheConfig(recommendationCacheService.loadConfig()));
    }

    /**
     * 使用已加载的缓存配置写入推荐缓存，避免同一请求内重复读取配置。
     */
    private void saveRecommendationCache(Long userId, RecommendationResponse response, RecommendationCacheConfigDTO cacheConfig) {
        if (userId == null || response == null || response.getList() == null || response.getList().isEmpty()) {
            return;
        }

        UserRecommendationCacheDTO cache = new UserRecommendationCacheDTO();
        cache.setType(response.getType());
        cache.setNeedPreference(response.getNeedPreference());
        cache.setGeneratedAt(System.currentTimeMillis());
        cache.setItems(response.getList().stream().map(item -> {
            UserRecommendationCacheDTO.CacheItem cacheItem = new UserRecommendationCacheDTO.CacheItem();
            cacheItem.setSpotId(item.getId());
            cacheItem.setScore(item.getScore());
            return cacheItem;
        }).collect(Collectors.toList()));

        recommendationCacheService.saveUserRecommendation(
            userId,
            cache,
            defaultInt(cacheConfig.getUserRecTTLMinutes(), 60)
        );
    }

    /**
     * 根据缓存快照重建推荐响应。
     */
    private RecommendationResponse buildRecommendationResponseFromCache(UserRecommendationCacheDTO cached, Integer limit) {
        if (cached == null || cached.getItems() == null || cached.getItems().isEmpty()) {
            return null;
        }

        List<Long> spotIds = cached.getItems().stream()
            .map(UserRecommendationCacheDTO.CacheItem::getSpotId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        if (spotIds.isEmpty()) {
            return null;
        }

        Map<Long, Double> scoreMap = cached.getItems().stream()
            .filter(item -> item.getSpotId() != null && item.getScore() != null)
            .collect(Collectors.toMap(
                UserRecommendationCacheDTO.CacheItem::getSpotId,
                UserRecommendationCacheDTO.CacheItem::getScore,
                (left, right) -> left,
                LinkedHashMap::new
            ));

        return buildRecommendationResponse(
            spotIds,
            scoreMap.isEmpty() ? null : scoreMap,
            limit,
            cached.getType(),
            Boolean.TRUE.equals(cached.getNeedPreference())
        );
    }

    /**
     * 预览缓存模式下的推荐结果；命中缓存时不重算，仅补齐最小调试信息。
     */
    private RecommendationResponse previewCachedRecommendations(Long userId, Integer limit, boolean debug) {
        if (!debug) {
            return getRecommendations(userId, limit);
        }

        UserRecommendationCacheDTO cached = recommendationCacheService.getUserRecommendation(userId);
        if (cached == null) {
            return computeRecommendations(userId, limit, false, true, false);
        }

        RecommendationResponse response = buildRecommendationResponseFromCache(cached, limit);
        if (response == null) {
            return computeRecommendations(userId, limit, false, true, false);
        }

        RecommendationResponse.DebugInfo debugInfo = initDebugInfo(userId, limit, PREVIEW_MODE_CACHE);
        debugInfo.setTriggerReason("命中当前推荐缓存");
        debugInfo.setFinalCount(response.getList() == null ? 0 : response.getList().size());
        debugInfo.setNotes(List.of(
            "当前结果直接来自用户推荐缓存，未触发重算。",
            "如需查看完整评分链路，请切换到重算稳定或重算后轮换模式。"
        ));
        response.setDebugInfo(debugInfo);
        return response;
    }

    /**
     * 规范化管理端预览模式，非法值统一回退为缓存模式。
     */
    private String normalizePreviewMode(String mode) {
        if (PREVIEW_MODE_RECOMPUTE.equals(mode) || PREVIEW_MODE_RECOMPUTE_ROTATE.equals(mode)) {
            return mode;
        }
        return PREVIEW_MODE_CACHE;
    }

    /**
     * 生成调试模式标识，便于后台直接理解本次预览策略。
     */
    private String normalizeDebugMode(boolean recompute, boolean rotateAfterCompute) {
        if (!recompute) {
            return PREVIEW_MODE_CACHE;
        }
        return rotateAfterCompute ? PREVIEW_MODE_RECOMPUTE_ROTATE : PREVIEW_MODE_RECOMPUTE;
    }

    /**
     * 便捷重载，默认不收集调试贡献明细。
     */
    private Map<Long, Double> computeItemCFRecommendationScores(Map<Long, Double> userInteractions, Integer limit) {
        return computeItemCFRecommendationScores(userInteractions, limit, null);
    }

    /**
     * 基于 ItemCF 计算候选景点分数。
     * <p>
     * 若传入贡献映射，则同时记录每个候选由哪些历史景点贡献了多少分值。
     */
    private Map<Long, Double> computeItemCFRecommendationScores(Map<Long, Double> userInteractions, Integer limit,
                                                                Map<Long, List<RecommendationResponse.DebugEntry>> contributionMap) {
        if (userInteractions.isEmpty()) {
            return Collections.emptyMap();
        }

        // 第 1 步：初始化候选分数累加器。
        // 每个候选景点最终分数，都是来自多个“历史景点 -> 相似景点”贡献的叠加结果。
        Map<Long, Double> scores = new HashMap<>();
        
        for (Map.Entry<Long, Double> entry : userInteractions.entrySet()) {
            Long spotId = entry.getKey();
            Double rui = entry.getValue(); // 融合后的交互权重

            // 第 2 步：读取当前历史景点的相似邻居。
            // 相似邻居由离线矩阵提前算好，在线链路只做查表和分数聚合，避免实时重算相似度。
            Map<Long, Double> similarities = recommendationSimilaritySupport.getSimilarSpots(spotId);
            
            for (Map.Entry<Long, Double> simEntry : similarities.entrySet()) {
                Long similarSpotId = simEntry.getKey();
                Double wji = simEntry.getValue(); // 相似度分数

                // 第 3 步：过滤用户已交互景点。
                // 这里做的是在线兜底，防止离线矩阵里存在有效邻居，但对当前用户来说已经消费过。
                if (userInteractions.containsKey(similarSpotId)) {
                    continue;
                }
                
                // 第 4 步：累加协同过滤贡献值。
                // 某个候选如果同时和多个历史景点相似，就会在这里不断叠加贡献，最终形成综合得分。
                double contribution = wji * rui;
                scores.merge(similarSpotId, contribution, Double::sum);
                if (contributionMap != null) {
                    // 调试模式额外记录贡献明细，后续可以直接解释“这个候选是被哪些历史行为推出来的”。
                    contributionMap.computeIfAbsent(similarSpotId, key -> new ArrayList<>())
                        .add(new RecommendationResponse.DebugEntry(
                            spotId,
                            getSpotName(spotId),
                            contribution,
                            String.format(Locale.ROOT, "由该历史景点贡献：相似度=%.4f，交互权重=%.4f", wji, rui)
                        ));
                }
            }
        }

        // 第 5 步：按分数截断候选集。
        // 这里只保留前 N 个候选，控制后续过滤、重排和响应组装的处理规模。
        return scores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(limit)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (left, right) -> left,
                LinkedHashMap::new
            ));
    }

    /**
     * 默认不输出调试日志的热度重排入口。
     */
    private Map<Long, Double> applyHeatRerank(Map<Long, Double> scoreMap, RecommendationHeatConfigDTO config) {
        return applyHeatRerank(scoreMap, config, false);
    }

    /**
     * 在个性化分数基础上叠加热度因子，生成最终展示排序。
     */
    private Map<Long, Double> applyHeatRerank(Map<Long, Double> scoreMap, RecommendationHeatConfigDTO config, boolean debug) {
        return recommendationScoreSupport.applyHeatRerank(scoreMap, config, debug);
    }

    /**
     * 将缓存中读取出的对象安全转换为 Long，兼容不同序列化结果。
     */
    private Long castToLong(Object value) {
        return recommendationScoreSupport.castToLong(value);
    }

    /**
     * 将原始缓存值转换为 Double，避免分数反序列化后类型不一致。
     */
    private Double castToDouble(Object value) {
        return recommendationScoreSupport.castToDouble(value);
    }

    /**
     * 过滤用户已经交互过的景点。
     * <p>
     * 该步骤与在线计算阶段的兜底过滤配合，确保最终结果只包含“用户尚未消费”的内容。
     */
    private List<Long> filterInteractedSpots(Long userId, List<Long> spotIds) {
        return recommendationScoreSupport.filterInteractedSpots(userId, spotIds);
    }


    // 热门与附近景点能力

    @Override
    public HotSpotResponse getHotSpots(Integer limit) {
        if (limit == null || limit <= 0) limit = 10;

        // 第 1 步：优先读取缓存。
        // 热门景点是首页和冷启动都会高频访问的公共能力，因此缓存命中率非常关键。
        HotSpotResponse cachedResponse = recommendationCacheService.getHomeHotSpots(limit);
        if (cachedResponse != null && cachedResponse.getList() != null) {
            return cachedResponse;
        }

        // 第 2 步：按热度分查询当前有效景点。
        // 这里只拉取已发布且未删除的景点，避免热门列表出现后台不可见内容。
        List<Spot> spots = spotMapper.selectList(
            new LambdaQueryWrapper<Spot>()
                .eq(Spot::getIsPublished, 1)
                .eq(Spot::getIsDeleted, 0)
                .orderByDesc(Spot::getHeatScore)
                .last("LIMIT " + limit)
        );

        // 第 3 步：补齐列表展示字段。
        // 热门接口只组装首页卡片所需字段，避免返回无用信息增加查询与序列化成本。
        Map<Long, String> categoryMap = getCategoryMap();

        HotSpotResponse response = new HotSpotResponse();
        response.setList(spots.stream().map(spot -> {
            HotSpotResponse.SpotItem item = new HotSpotResponse.SpotItem();
            item.setId(spot.getId());
            item.setName(spot.getName());
            item.setCoverImage(spot.getCoverImageUrl());
            item.setPrice(spot.getPrice());
            item.setAvgRating(spot.getAvgRating());
            item.setHeatScore(spot.getHeatScore());
            item.setCategoryName(categoryMap.get(spot.getCategoryId()));
            return item;
        }).collect(Collectors.toList()));

        // 第 4 步：回写缓存。
        // 热门结果变化频率相对较低，适合缓存后被首页和冷启动逻辑共同复用。
        recommendationCacheService.saveHomeHotSpots(limit, response);

        return response;
    }

    @Override
    public RecentViewedSpotResponse getRecentViewedSpots(Integer days, Integer limit) {
        int safeDays = days == null || days <= 0 ? 14 : days;
        int safeLimit = limit == null || limit <= 0 ? 12 : limit;

        // 最近浏览直接按时间窗口读取聚合结果，保持首页“最近看过”模块轻量。
        List<RecentViewedSpotItem> list = userSpotViewMapper.selectRecentViewedSpots(
            LocalDateTime.now().minusDays(safeDays),
            safeLimit
        );

        RecentViewedSpotResponse response = new RecentViewedSpotResponse();
        response.setList(list);
        response.setDays(safeDays);
        return response;
    }

    @Override
    public NearbySpotResponse getNearbySpots(BigDecimal latitude, BigDecimal longitude, Integer limit) {
        if (latitude == null || longitude == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "经纬度不能为空");
        }
        if (limit == null || limit <= 0) limit = 3;

        // 距离排序由 SQL 层完成，这里只补齐展示层需要的格式化处理。
        List<NearbySpotResponse.SpotItem> list = spotMapper.selectNearbySpots(latitude, longitude, limit);
        list.forEach(item -> {
            if (item.getDistanceKm() != null) {
                // 前端展示统一保留一位小数，避免返回过长的小数尾数影响可读性。
                item.setDistanceKm(item.getDistanceKm().setScale(1, RoundingMode.HALF_UP));
            }
        });

        NearbySpotResponse response = new NearbySpotResponse();
        response.setList(list);
        response.setTotal(list.size());
        response.setNearestDistanceKm(list.isEmpty() ? null : list.get(0).getDistanceKm());
        return response;
    }

    // 相似度矩阵离线更新

    /**
     * 离线重建景点相似度矩阵。
     * <p>
     * 核心流程包括：收集有效景点、构建用户-景点交互矩阵、计算相似邻居、缓存 Top-K 结果并刷新状态摘要。
     */
    @Override
    public void updateSimilarityMatrix() {
        // computing 作为进程内互斥标记，防止后台重复触发离线任务导致缓存覆盖和资源浪费。
        if (!computing.compareAndSet(false, true)) {
            log.warn("相似度矩阵正在更新中，跳过重复请求");
            return;
        }

        try {
            // 第 1 步：加载配置并确认本次任务的输入范围。
            RecommendationConfigBundleDTO config = recommendationCacheService.loadConfig();
            RecommendationAlgorithmConfigDTO algorithmConfig = safeAlgorithmConfig(config);
            RecommendationCacheConfigDTO cacheConfig = safeCacheConfig(config);
            log.info("开始更新相似度矩阵");
            Set<Long> activeSpotIds = recommendationSimilaritySupport.getActiveSpotIds();
            if (activeSpotIds.isEmpty()) {
                log.info("没有有效上架景点，跳过相似度矩阵更新");
                return;
            }

            // 第 2 步：构建离线快照。
            // 快照同时提供用户-景点交互矩阵和参与计算的景点全集，便于后续步骤统一复用。
            RecommendationSimilaritySupport.OfflineMatrixSnapshot snapshot =
                recommendationSimilaritySupport.buildOfflineInteractionMatrix(activeSpotIds, algorithmConfig);
            Map<Long, Map<Long, Double>> userItemMatrix = snapshot.userItemMatrix();
            Set<Long> allSpotIds = snapshot.allSpotIds();

            if (userItemMatrix.isEmpty()) {
                log.info("没有交互数据，跳过相似度矩阵更新");
                return;
            }

            log.info("交互矩阵构建完成：用户数={}，景点数={}", userItemMatrix.size(), allSpotIds.size());

            // 第 3 步：预计算用户活跃度。
            // IUF 会降低高活跃用户对相似度的放大效应，因此这里先汇总每个用户的交互规模。
            Map<Long, Integer> userActivityCount = recommendationSimilaritySupport.summarizeUserActivityCount(userItemMatrix);

            // 第 4 步：构建景点倒排索引。
            // 这样后续在计算景点两两相似度时，可以快速定位共同交互用户集合。
            Map<Long, Set<Long>> spotUserSets = recommendationSimilaritySupport.buildSpotUserIndex(userItemMatrix);

            // 第 5 步：计算并缓存 Top-K 相似邻居。
            // 在线推荐只读裁剪后的邻居结果，离线阶段把重计算成本一次性承担掉。
            int topK = defaultInt(algorithmConfig.getTopKNeighbors(), 20);
            int simTTL = defaultInt(cacheConfig.getSimilarityTTLHours(), 24);
            recommendationSimilaritySupport.cacheSimilarityNeighbors(
                allSpotIds,
                spotUserSets,
                userActivityCount,
                algorithmConfig,
                cacheConfig
            );

            // 第 6 步：保存任务摘要并失效依赖缓存。
            // 相似度矩阵更新后，旧的个性化推荐结果已经不再代表当前模型状态，需要统一清理。
            recommendationSimilaritySupport.saveOfflineSummary(userItemMatrix.size(), allSpotIds.size());

            log.info(
                "相似度矩阵更新完成：景点数={}，用户数={}，缓存时长={}小时，Top-K={}",
                allSpotIds.size(),
                userItemMatrix.size(),
                simTTL,
                topK
            );
            invalidateGlobalRecommendationCaches();
        } finally {
            // 无论成功还是失败，都必须释放互斥标记，避免后续任务永久无法执行。
            computing.set(false);
        }
    }

    /**
     * 提供默认 double 值，避免配置项为空时在计算阶段反复判空。
     */
    private double defaultDouble(Double value, double fallback) {
        return value == null ? fallback : value;
    }

    /**
     * 提供默认 int 值，统一收口推荐参数的空值兜底。
     */
    private int defaultInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    /**
     * 安全读取算法配置，确保配置缺失时主流程仍可用默认参数运行。
     */
    private RecommendationAlgorithmConfigDTO safeAlgorithmConfig(RecommendationConfigBundleDTO config) {
        return config == null || config.getAlgorithm() == null
            ? new RecommendationAlgorithmConfigDTO()
            : config.getAlgorithm();
    }

    /**
     * 安全读取热度重排配置，避免后台配置未初始化时抛空指针。
     */
    private RecommendationHeatConfigDTO safeHeatConfig(RecommendationConfigBundleDTO config) {
        return config == null || config.getHeat() == null
            ? new RecommendationHeatConfigDTO()
            : config.getHeat();
    }

    /**
     * 安全读取缓存配置，保证 TTL 相关逻辑始终有可用默认值。
     */
    private RecommendationCacheConfigDTO safeCacheConfig(RecommendationConfigBundleDTO config) {
        return config == null || config.getCache() == null
            ? new RecommendationCacheConfigDTO()
            : config.getCache();
    }

    /**
     * 计算个性化候选扩容倍数，至少保证为 1，避免候选集被错误压缩成空。
     */
    private int getCandidateExpandFactor(RecommendationAlgorithmConfigDTO config) {
        return Math.max(defaultInt(config.getCandidateExpandFactor(), 2), 1);
    }

    /**
     * 计算冷启动候选扩容倍数，便于偏好过滤后仍能保留足够结果。
     */
    private int getColdStartExpandFactor(RecommendationAlgorithmConfigDTO config) {
        return Math.max(defaultInt(config.getColdStartExpandFactor(), 3), 1);
    }

    // 推荐结果组装与元数据补充

    /**
     * 最简组装入口，适用于无分数、无调试信息的普通结果返回。
     */
    private RecommendationResponse buildRecommendationResponse(List<Long> spotIds, Integer limit, String type, Boolean needPreference) {
        return buildRecommendationResponse(spotIds, null, limit, type, needPreference, null);
    }

    /**
     * 组装带分数的推荐结果，供个性化推荐场景复用。
     */
    private RecommendationResponse buildRecommendationResponse(List<Long> spotIds, Map<Long, Double> scoreMap, Integer limit, String type, Boolean needPreference) {
        return buildRecommendationResponse(spotIds, scoreMap, limit, type, needPreference, null);
    }

    /**
     * 推荐结果统一组装入口。
     * <p>
     * 具体的景点信息填充交给查询支持类完成，服务层只负责传递推荐元数据。
     */
    private RecommendationResponse buildRecommendationResponse(List<Long> spotIds, Map<Long, Double> scoreMap,
                                                               Integer limit, String type, Boolean needPreference,
                                                               RecommendationResponse.DebugInfo debugInfo) {
        return recommendationQuerySupport.buildRecommendationResponse(
            spotIds,
            scoreMap,
            limit,
            type,
            needPreference,
            debugInfo
        );
    }

    /**
     * 组装带调试信息但无分数映射的推荐结果，主要用于冷启动和预览场景。
     */
    private RecommendationResponse buildRecommendationResponse(List<Long> spotIds, Integer limit, String type, Boolean needPreference,
                                                               RecommendationResponse.DebugInfo debugInfo) {
        return buildRecommendationResponse(spotIds, null, limit, type, needPreference, debugInfo);
    }

    /**
     * 获取分类名称映射，供热门和推荐结果补齐展示字段复用。
     */
    private Map<Long, String> getCategoryMap() {
        return recommendationQuerySupport.getCategoryMap();
    }

    /**
     * 获取地区名称映射，保留给推荐结果补充地域元数据使用。
     */
    private Map<Long, String> getRegionMap() {
        return recommendationQuerySupport.getRegionMap();
    }

    /**
     * 输出推荐预览日志，便于后台查看预览接口的实际命中结果。
     */
    private void logRecommendationPreview(Long userId, RecommendationResponse response, String mode) {
        recommendationScoreSupport.logRecommendationPreview(userId, response, mode);
    }

    // 调试信息组装与日志输出

    /**
     * 初始化调试载体，承接本次推荐流程中的各阶段诊断信息。
     */
    private RecommendationResponse.DebugInfo initDebugInfo(Long userId, Integer limit, String mode) {
        RecommendationResponse.DebugInfo debugInfo = recommendationScoreSupport.initDebugInfo(userId, limit, mode);
        debugInfo.setUserNickname(resolveRecommendationDebugNickname(userId));
        return debugInfo;
    }

    /**
     * 调试预览直接回显目标用户昵称，方便后台确认当前查看的是谁的推荐结果。
     */
    private String resolveRecommendationDebugNickname(Long userId) {
        if (userId == null) {
            return ResourceDisplayText.User.UNKNOWN;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return ResourceDisplayText.User.PURGED;
        }
        if (user.getIsDeleted() != null && user.getIsDeleted() == 1) {
            return ResourceDisplayText.User.DEACTIVATED;
        }
        if (user.getNickname() == null || user.getNickname().isBlank()) {
            return "未命名用户";
        }
        return user.getNickname();
    }

    /**
     * 填充用户交互权重明细，便于调试时核对兴趣输入是否符合预期。
     */
    private void populateInteractionDebugInfo(RecommendationResponse.DebugInfo debugInfo, Map<Long, Double> userInteractions) {
        recommendationScoreSupport.populateInteractionDebugInfoDetailed(debugInfo, userInteractions);
    }

    /**
     * 填充行为统计摘要，例如浏览、收藏、评分和订单数量。
     */
    private void populateBehaviorStats(RecommendationResponse.DebugInfo debugInfo, Long userId) {
        recommendationScoreSupport.populateBehaviorStats(debugInfo, userId);
    }

    /**
     * 填充行为详情，帮助后台进一步分析各行为权重的组成来源。
     */
    private void populateBehaviorDetails(RecommendationResponse.DebugInfo debugInfo, Long userId,
                                         RecommendationAlgorithmConfigDTO config) {
        recommendationScoreSupport.populateBehaviorDetails(debugInfo, userId, config);
    }

    /**
     * 记录某一评分阶段的候选分数快照。
     */
    private void populateScoreDebugEntries(RecommendationResponse.DebugInfo debugInfo, Map<Long, Double> scores, String description) {
        recommendationScoreSupport.populateScoreDebugEntries(debugInfo, scores, description);
    }

    /**
     * 记录过滤后的候选分数，便于对比过滤前后的差异。
     */
    private void populateFilteredScoresDebugEntries(RecommendationResponse.DebugInfo debugInfo, Map<Long, Double> scores, String description) {
        recommendationScoreSupport.populateFilteredScoresDebugEntries(debugInfo, scores, description);
    }

    /**
     * 记录热度重排后的分数结果，辅助分析热度因子的影响幅度。
     */
    private void populateRerankedScoreDebugEntries(RecommendationResponse.DebugInfo debugInfo, Map<Long, Double> scores, String description) {
        recommendationScoreSupport.populateRerankedScoreDebugEntries(debugInfo, scores, description);
    }

    /**
     * 记录被过滤掉的候选景点，便于解释结果为什么发生变化。
     */
    private void populateFilteredOutDebugEntries(RecommendationResponse.DebugInfo debugInfo, List<Long> originalIds,
                                                 List<Long> filteredIds, String description) {
        recommendationScoreSupport.populateFilteredOutDebugEntries(debugInfo, originalIds, filteredIds, description);
    }

    /**
     * 将候选贡献明细整理成最终结果贡献列表，服务于“推荐解释”展示。
     */
    private List<RecommendationResponse.ResultContribution> buildResultContributions(
        Map<Long, Double> finalScores,
        Map<Long, List<RecommendationResponse.DebugEntry>> contributionMap
    ) {
        return recommendationScoreSupport.buildResultContributions(finalScores, contributionMap);
    }

    /**
     * 输出用户交互权重日志，调试开启时可快速定位输入行为是否异常。
     */
    private void logUserInteractionWeights(Long userId, Map<Long, Double> userInteractions, boolean debug) {
        recommendationScoreSupport.logUserInteractionWeights(userId, userInteractions, debug);
    }

    /**
     * 输出指定阶段的候选分数日志，便于观察排序变化。
     */
    private void logScoreMap(String stage, Map<Long, Double> scoreMap, boolean debug) {
        recommendationScoreSupport.logScoreMap(stage, scoreMap, debug);
    }

    /**
     * 输出过滤前后候选差异，辅助分析为什么某些景点没有进入最终结果。
     */
    private void logFilteredRecommendations(Long userId, List<Long> originalIds, List<Long> filteredIds, boolean debug) {
        recommendationScoreSupport.logFilteredRecommendations(userId, originalIds, filteredIds, debug);
    }

    /**
     * 输出冷启动命中路径和结果摘要，便于区分偏好推荐与热门兜底。
     */
    private void logColdStartResult(Long userId, String type, List<Long> categoryIds, List<Long> spotIds, boolean debug) {
        recommendationScoreSupport.logColdStartResult(userId, type, categoryIds, spotIds, debug);
    }

    /**
     * 输出热度重排细节，当前由支持类承接具体日志格式。
     */
    private void logHeatRerankDetails(Map<Long, Double> beforeScores, Map<Long, Double> afterScores,
                                      Map<Long, Integer> heatMap, double rerankFactor, int maxHeat) {
        recommendationScoreSupport.logHeatRerankDetails(beforeScores, afterScores, heatMap, rerankFactor, maxHeat);
    }

    /**
     * 根据景点 ID 查询名称，主要用于调试贡献说明的人类可读输出。
     */
    private String getSpotName(Long spotId) {
        return recommendationQuerySupport.getSpotName(spotId);
    }

    /**
     * 按给定景点顺序重排分数映射，保证列表顺序与分数顺序一致。
     */
    private Map<Long, Double> orderScoresByIds(List<Long> orderedIds, Map<Long, Double> scoreMap) {
        return recommendationScoreSupport.orderScoresByIds(orderedIds, scoreMap);
    }

    /**
     * 将缓存中的原始分数映射转换成强类型结构，兼容不同序列化场景。
     */
    private Map<Long, Double> castScoreMap(Map<?, ?> rawMap) {
        return recommendationScoreSupport.castScoreMap(rawMap);
    }

    // 配置管理与状态查询

    @Override
    public RecommendationConfigBundleDTO getConfig() {
        return recommendationConfigSupport.getConfig();
    }

    @Override
    public void updateConfig(RecommendationConfigBundleDTO config) {
        // 配置更新后需要同步清理推荐缓存，避免新旧参数混用导致结果不可解释。
        recommendationConfigSupport.updateConfig(config);
        invalidateGlobalRecommendationCaches();
        log.info("推荐算法配置已更新 {}", config);
    }

    @Override
    public RecommendationStatusDTO getStatus() {
        // 状态接口统一复用 support 汇总信息，仅在这里补充当前计算标志。
        return recommendationConfigSupport.buildStatus(computing.get());
    }

    @Override
    public SimilarityPreviewResponse previewSimilarityNeighbors(Long spotId, Integer limit) {
        // 相似景点预览需要携带最近更新时间，便于判断预览结果对应哪次离线矩阵。
        return recommendationSimilaritySupport.buildSimilarityPreview(spotId, limit, getStatus().getLastUpdateTime());
    }
}


