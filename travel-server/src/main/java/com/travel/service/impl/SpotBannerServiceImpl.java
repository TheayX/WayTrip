package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.travel.dto.banner.request.AdminBannerRequest;
import com.travel.dto.banner.response.AdminBannerListResponse;
import com.travel.dto.banner.response.BannerResponse;
import com.travel.entity.SpotBanner;
import com.travel.entity.Spot;
import com.travel.mapper.SpotBannerMapper;
import com.travel.mapper.SpotMapper;
import com.travel.service.SpotBannerService;
import com.travel.service.cache.RecommendationCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 景点轮播图服务实现，负责用户端展示与管理端维护逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpotBannerServiceImpl implements SpotBannerService {

    // 持久层依赖

    private final SpotBannerMapper spotBannerMapper;
    private final SpotMapper spotMapper;
    private final RecommendationCacheService recommendationCacheService;

    // 用户端展示接口

    @Override
    public BannerResponse getBanners() {
        BannerResponse cachedResponse = recommendationCacheService.getHomeBanners();
        if (cachedResponse != null && cachedResponse.getList() != null) {
            return cachedResponse;
        }

        List<SpotBanner> banners = spotBannerMapper.selectEnabledBanners();

        BannerResponse response = new BannerResponse();
        response.setList(banners.stream().map(banner -> {
            BannerResponse.BannerItem item = new BannerResponse.BannerItem();
            item.setId(banner.getId());
            item.setImageUrl(banner.getImageUrl());
            item.setSpotId(banner.getSpotId());
            item.setSpotName(banner.getSpotName());
            item.setSortOrder(banner.getSortOrder());
            return item;
        }).collect(Collectors.toList()));

        recommendationCacheService.saveHomeBanners(response);

        return response;
    }

    // 管理端维护接口

    @Override
    public AdminBannerListResponse getAdminBanners() {
        List<SpotBanner> banners = spotBannerMapper.selectList(
            new LambdaQueryWrapper<SpotBanner>()
                .eq(SpotBanner::getIsDeleted, 0)
                .orderByAsc(SpotBanner::getSortOrder)
        );

        Map<Long, String> spotNameMap = getSpotNameMap(banners);

        AdminBannerListResponse response = new AdminBannerListResponse();
        response.setList(banners.stream().map(banner -> {
            AdminBannerListResponse.BannerItem item = new AdminBannerListResponse.BannerItem();
            item.setId(banner.getId());
            item.setImageUrl(banner.getImageUrl());
            item.setSpotId(banner.getSpotId());
            item.setSpotName(spotNameMap.get(banner.getSpotId()));
            item.setSortOrder(banner.getSortOrder());
            item.setEnabled(banner.getIsEnabled());
            item.setCreatedAt(banner.getCreatedAt());
            item.setUpdatedAt(banner.getUpdatedAt());
            return item;
        }).collect(Collectors.toList()));
        response.setTotal((long) banners.size());

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createBanner(AdminBannerRequest request) {
        AdminBannerRequest validRequest = Objects.requireNonNull(request);
        int targetSortOrder = prepareInsertSortOrder(validRequest.getSortOrder());
        SpotBanner banner = new SpotBanner();
        banner.setImageUrl(validRequest.getImageUrl());
        banner.setSpotId(validRequest.getSpotId());
        banner.setSortOrder(targetSortOrder);
        banner.setIsEnabled(validRequest.getEnabled());
        spotBannerMapper.insert(banner);
        recommendationCacheService.deleteHomeBanners();
        log.info("轮播图创建成功: bannerId={}, spotId={}", banner.getId(), banner.getSpotId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBanner(Long id, AdminBannerRequest request) {
        AdminBannerRequest validRequest = Objects.requireNonNull(request);
        SpotBanner banner = getActiveBanner(id);
        int targetSortOrder = prepareUpdateSortOrder(banner, validRequest.getSortOrder());

        spotBannerMapper.update(
            null,
            new LambdaUpdateWrapper<SpotBanner>()
                .eq(SpotBanner::getId, id)
                .set(SpotBanner::getImageUrl, validRequest.getImageUrl())
                .set(SpotBanner::getSpotId, validRequest.getSpotId())
                .set(SpotBanner::getSortOrder, targetSortOrder)
                .set(SpotBanner::getIsEnabled, validRequest.getEnabled())
        );
        recommendationCacheService.deleteHomeBanners();
        log.info("轮播图更新成功: bannerId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBanner(Long id) {
        SpotBanner banner = getActiveBanner(id);
        int removedSortOrder = safeSortOrder(banner.getSortOrder());
        banner.setIsDeleted(1);
        spotBannerMapper.updateById(banner);
        compactSortOrdersAfterRemoval(id, removedSortOrder);
        recommendationCacheService.deleteHomeBanners();
        log.info("轮播图已删除: bannerId={}", id);
    }

    @Override
    public void toggleEnabled(Long id) {
        SpotBanner banner = getActiveBanner(id);

        banner.setIsEnabled(banner.getIsEnabled() == 1 ? 0 : 1);
        spotBannerMapper.updateById(banner);
        recommendationCacheService.deleteHomeBanners();
        log.info("轮播图启用状态切换: bannerId={}, enabled={}", id, banner.getIsEnabled());
    }

    // 内部转换与补充方法

    /**
     * 管理端编辑、删除和启停都要求轮播图仍然有效，统一收口存在性校验。
     */
    private SpotBanner getActiveBanner(Long id) {
        SpotBanner banner = spotBannerMapper.selectById(id);
        if (banner == null || banner.getIsDeleted() == 1) {
            throw new RuntimeException("轮播图不存在");
        }
        return banner;
    }

    /**
     * 新增轮播图时自动规范目标排序，并对后续项顺延。
     */
    private int prepareInsertSortOrder(Integer requestedSortOrder) {
        List<SpotBanner> banners = listActiveBanners();
        int maxSortOrder = banners.stream()
            .map(SpotBanner::getSortOrder)
            .filter(Objects::nonNull)
            .max(Integer::compareTo)
            .orElse(0);
        int targetSortOrder = normalizeInsertSortOrder(requestedSortOrder, maxSortOrder);
        if (targetSortOrder <= maxSortOrder) {
            banners.stream()
                .filter(item -> safeSortOrder(item.getSortOrder()) >= targetSortOrder)
                .sorted((left, right) -> Integer.compare(safeSortOrder(right.getSortOrder()), safeSortOrder(left.getSortOrder())))
                .forEach(item -> {
                    item.setSortOrder(safeSortOrder(item.getSortOrder()) + 1);
                    spotBannerMapper.updateById(item);
                });
        }
        return targetSortOrder;
    }

    /**
     * 编辑轮播图时，只移动受影响区间，避免无关序号变化。
     */
    private int prepareUpdateSortOrder(SpotBanner currentBanner, Integer requestedSortOrder) {
        int originalSortOrder = safeSortOrder(currentBanner.getSortOrder());
        List<SpotBanner> siblings = listActiveBanners().stream()
            .filter(item -> !Objects.equals(item.getId(), currentBanner.getId()))
            .collect(Collectors.toList());
        int maxSortOrder = siblings.stream()
            .map(SpotBanner::getSortOrder)
            .filter(Objects::nonNull)
            .max(Integer::compareTo)
            .orElse(0);
        int targetSortOrder = normalizeUpdateSortOrder(requestedSortOrder, maxSortOrder + 1);

        if (targetSortOrder < originalSortOrder) {
            siblings.stream()
                .filter(item -> {
                    int sortOrder = safeSortOrder(item.getSortOrder());
                    return sortOrder >= targetSortOrder && sortOrder < originalSortOrder;
                })
                .sorted((left, right) -> Integer.compare(safeSortOrder(right.getSortOrder()), safeSortOrder(left.getSortOrder())))
                .forEach(item -> {
                    item.setSortOrder(safeSortOrder(item.getSortOrder()) + 1);
                    spotBannerMapper.updateById(item);
                });
        } else if (targetSortOrder > originalSortOrder) {
            siblings.stream()
                .filter(item -> {
                    int sortOrder = safeSortOrder(item.getSortOrder());
                    return sortOrder > originalSortOrder && sortOrder <= targetSortOrder;
                })
                .sorted((left, right) -> Integer.compare(safeSortOrder(left.getSortOrder()), safeSortOrder(right.getSortOrder())))
                .forEach(item -> {
                    item.setSortOrder(safeSortOrder(item.getSortOrder()) - 1);
                    spotBannerMapper.updateById(item);
                });
        }

        return targetSortOrder;
    }

    /**
     * 删除轮播图后补齐排序空位，保证前台轮播顺序连续。
     */
    private void compactSortOrdersAfterRemoval(Long currentId, int removedSortOrder) {
        listActiveBanners().stream()
            .filter(item -> !Objects.equals(item.getId(), currentId))
            .filter(item -> safeSortOrder(item.getSortOrder()) > removedSortOrder)
            .sorted((left, right) -> Integer.compare(safeSortOrder(left.getSortOrder()), safeSortOrder(right.getSortOrder())))
            .forEach(item -> {
                item.setSortOrder(safeSortOrder(item.getSortOrder()) - 1);
                spotBannerMapper.updateById(item);
            });
    }

    /**
     * 查询当前所有有效轮播图，作为重排基准数据。
     */
    private List<SpotBanner> listActiveBanners() {
        return spotBannerMapper.selectList(
            new LambdaQueryWrapper<SpotBanner>()
                .eq(SpotBanner::getIsDeleted, 0)
                .orderByAsc(SpotBanner::getSortOrder)
                .orderByAsc(SpotBanner::getId)
        );
    }

    /**
     * 新增时允许插入中间，超出范围自动落到最后。
     */
    private int normalizeInsertSortOrder(Integer requestedSortOrder, int maxSortOrder) {
        if (requestedSortOrder == null) {
            return maxSortOrder + 1;
        }
        int normalized = Math.max(1, requestedSortOrder);
        return Math.min(normalized, maxSortOrder + 1);
    }

    /**
     * 编辑时保留重排能力，同时限制在可用区间内。
     */
    private int normalizeUpdateSortOrder(Integer requestedSortOrder, int maxSortOrder) {
        if (requestedSortOrder == null) {
            return maxSortOrder;
        }
        int normalized = Math.max(1, requestedSortOrder);
        return Math.min(normalized, Math.max(1, maxSortOrder));
    }

    /**
     * 兼容历史空排序数据，避免重排时空指针。
     */
    private int safeSortOrder(Integer sortOrder) {
        return sortOrder == null ? 1 : sortOrder;
    }

    private Map<Long, String> getSpotNameMap(List<SpotBanner> banners) {
        List<Long> spotIds = banners.stream()
            .map(SpotBanner::getSpotId)
            .filter(id -> id != null)
            .distinct()
            .collect(Collectors.toList());

        if (spotIds.isEmpty()) {
            return Map.of();
        }

        return spotMapper.selectBatchIds(spotIds).stream()
            .filter(spot -> spot.getIsDeleted() == 0)
            .collect(Collectors.toMap(Spot::getId, Spot::getName));
    }
}

