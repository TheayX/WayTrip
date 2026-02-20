package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.dto.banner.*;
import com.travel.entity.Banner;
import com.travel.entity.Spot;
import com.travel.mapper.BannerMapper;
import com.travel.mapper.SpotMapper;
import com.travel.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private final BannerMapper bannerMapper;
    private final SpotMapper spotMapper;

    @Override
    public BannerResponse getBanners() {
        List<Banner> banners = bannerMapper.selectEnabledBanners();

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
        List<Banner> banners = bannerMapper.selectList(
            new LambdaQueryWrapper<Banner>()
                .eq(Banner::getIsDeleted, 0)
                .orderByAsc(Banner::getSortOrder)
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
            item.setEnabled(banner.getEnabled());
            item.setCreatedAt(banner.getCreatedAt());
            item.setUpdatedAt(banner.getUpdatedAt());
            return item;
        }).collect(Collectors.toList()));
        response.setTotal((long) banners.size());

        return response;
    }

    @Override
    public void createBanner(AdminBannerRequest request) {
        Banner banner = new Banner();
        banner.setImageUrl(request.getImageUrl());
        banner.setSpotId(request.getSpotId());
        banner.setSortOrder(request.getSortOrder());
        banner.setEnabled(request.getEnabled());
        bannerMapper.insert(banner);
    }

    @Override
    public void updateBanner(Long id, AdminBannerRequest request) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null || banner.getIsDeleted() == 1) {
            throw new RuntimeException("轮播图不存在");
        }

        banner.setImageUrl(request.getImageUrl());
        banner.setSpotId(request.getSpotId());
        banner.setSortOrder(request.getSortOrder());
        banner.setEnabled(request.getEnabled());
        bannerMapper.updateById(banner);
    }

    @Override
    public void deleteBanner(Long id) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null || banner.getIsDeleted() == 1) {
            throw new RuntimeException("轮播图不存在");
        }
        banner.setIsDeleted(1);
        bannerMapper.updateById(banner);
    }

    @Override
    public void toggleEnabled(Long id) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null || banner.getIsDeleted() == 1) {
            throw new RuntimeException("轮播图不存在");
        }

        banner.setEnabled(banner.getEnabled() == 1 ? 0 : 1);
        bannerMapper.updateById(banner);
    }

    private Map<Long, String> getSpotNameMap(List<Banner> banners) {
        List<Long> spotIds = banners.stream()
            .map(Banner::getSpotId)
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
