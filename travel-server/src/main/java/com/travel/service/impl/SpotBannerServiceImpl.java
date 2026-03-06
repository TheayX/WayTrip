package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.dto.banner.*;
import com.travel.entity.SpotBanner;
import com.travel.entity.Spot;
import com.travel.mapper.SpotBannerMapper;
import com.travel.mapper.SpotMapper;
import com.travel.service.SpotBannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpotBannerServiceImpl implements SpotBannerService {

    private final SpotBannerMapper spotBannerMapper;
    private final SpotMapper spotMapper;

    @Override
    public BannerResponse getBanners() {
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

        return response;
    }

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
    public void createBanner(AdminBannerRequest request) {
        SpotBanner banner = new SpotBanner();
        banner.setImageUrl(request.getImageUrl());
        banner.setSpotId(request.getSpotId());
        banner.setSortOrder(request.getSortOrder());
        banner.setIsEnabled(request.getEnabled());
        spotBannerMapper.insert(banner);
        log.info("轮播图创建成功: bannerId={}, spotId={}", banner.getId(), banner.getSpotId());
    }

    @Override
    public void updateBanner(Long id, AdminBannerRequest request) {
        SpotBanner banner = spotBannerMapper.selectById(id);
        if (banner == null || banner.getIsDeleted() == 1) {
            throw new RuntimeException("轮播图不存在");
        }

        banner.setImageUrl(request.getImageUrl());
        banner.setSpotId(request.getSpotId());
        banner.setSortOrder(request.getSortOrder());
        banner.setIsEnabled(request.getEnabled());
        spotBannerMapper.updateById(banner);
        log.info("轮播图更新成功: bannerId={}", id);
    }

    @Override
    public void deleteBanner(Long id) {
        SpotBanner banner = spotBannerMapper.selectById(id);
        if (banner == null || banner.getIsDeleted() == 1) {
            throw new RuntimeException("轮播图不存在");
        }
        banner.setIsDeleted(1);
        spotBannerMapper.updateById(banner);
        log.info("轮播图已删除: bannerId={}", id);
    }

    @Override
    public void toggleEnabled(Long id) {
        SpotBanner banner = spotBannerMapper.selectById(id);
        if (banner == null || banner.getIsDeleted() == 1) {
            throw new RuntimeException("轮播图不存在");
        }

        banner.setIsEnabled(banner.getIsEnabled() == 1 ? 0 : 1);
        spotBannerMapper.updateById(banner);
        log.info("轮播图启用状态切换: bannerId={}, enabled={}", id, banner.getIsEnabled());
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

