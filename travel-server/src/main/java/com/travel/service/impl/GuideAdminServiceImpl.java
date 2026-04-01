package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.PageResult;
import com.travel.common.result.ResultCode;
import com.travel.dto.guide.request.AdminGuideListRequest;
import com.travel.dto.guide.request.AdminGuideRequest;
import com.travel.dto.guide.response.AdminGuideListResponse;
import com.travel.entity.Guide;
import com.travel.entity.GuideSpotRelation;
import com.travel.entity.Spot;
import com.travel.mapper.GuideMapper;
import com.travel.mapper.GuideSpotRelationMapper;
import com.travel.mapper.SpotMapper;
import com.travel.service.GuideAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 攻略管理服务实现，负责后台列表、详情和维护操作。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuideAdminServiceImpl implements GuideAdminService {

    private final GuideMapper guideMapper;
    private final GuideSpotRelationMapper guideSpotRelationMapper;
    private final SpotMapper spotMapper;

    @Override
    public PageResult<AdminGuideListResponse> getAdminGuideList(AdminGuideListRequest request) {
        Page<Guide> page = new Page<>(request.getPage(), request.getPageSize());
        LambdaQueryWrapper<Guide> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Guide::getIsDeleted, 0);

        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.like(Guide::getTitle, request.getKeyword());
        }
        if (StringUtils.hasText(request.getCategory())) {
            wrapper.eq(Guide::getCategory, request.getCategory());
        }
        if (request.getPublished() != null) {
            wrapper.eq(Guide::getIsPublished, request.getPublished());
        }
        wrapper.orderByAsc(Guide::getId);

        Page<Guide> result = guideMapper.selectPage(page, wrapper);
        List<AdminGuideListResponse> list = result.getRecords().stream()
            .map(this::convertToAdminListResponse)
            .collect(Collectors.toList());
        return PageResult.of(list, result.getTotal(), request.getPage(), request.getPageSize());
    }

    @Override
    public AdminGuideRequest getAdminGuideDetail(Long guideId) {
        Guide guide = getExistingGuide(guideId);
        List<GuideSpotRelation> guideSpots = guideSpotRelationMapper.selectList(
            new LambdaQueryWrapper<GuideSpotRelation>()
                .eq(GuideSpotRelation::getGuideId, guideId)
                .eq(GuideSpotRelation::getIsDeleted, 0)
                .orderByAsc(GuideSpotRelation::getSortOrder)
                .orderByAsc(GuideSpotRelation::getId)
        );
        List<Long> spotIds = guideSpots.stream()
            .map(GuideSpotRelation::getSpotId)
            .collect(Collectors.toList());

        List<AdminGuideRequest.SpotOption> spotOptions = new ArrayList<>();
        if (!spotIds.isEmpty()) {
            spotOptions = spotMapper.selectBatchIds(spotIds).stream()
                .map(this::convertToSpotOption)
                .collect(Collectors.toList());
        }

        List<Long> filteredSpotIds = spotOptions.stream()
            .filter(option -> option.getIsDeleted() == null || option.getIsDeleted() != 1)
            .map(AdminGuideRequest.SpotOption::getId)
            .collect(Collectors.toList());

        AdminGuideRequest response = new AdminGuideRequest();
        response.setTitle(guide.getTitle());
        response.setCoverImage(guide.getCoverImageUrl());
        response.setCategory(guide.getCategory());
        response.setContent(guide.getContent());
        response.setPublished(guide.getIsPublished() == 1);
        response.setSpotIds(filteredSpotIds);
        response.setSpotOptions(spotOptions);
        return response;
    }

    @Override
    @Transactional
    public Long createGuide(AdminGuideRequest request, Long adminId) {
        Guide guide = new Guide();
        guide.setTitle(request.getTitle());
        guide.setCoverImageUrl(request.getCoverImage());
        guide.setCategory(request.getCategory());
        guide.setContent(request.getContent());
        guide.setAdminId(adminId);
        guide.setIsPublished(Boolean.TRUE.equals(request.getPublished()) ? 1 : 0);
        guide.setViewCount(0);
        guideMapper.insert(guide);

        saveGuideSpots(guide.getId(), request.getSpotIds());
        log.info("攻略创建成功: guideId={}, title={}, adminId={}", guide.getId(), guide.getTitle(), adminId);
        return guide.getId();
    }

    @Override
    @Transactional
    public void updateGuide(Long guideId, AdminGuideRequest request) {
        Guide guide = getExistingGuide(guideId);
        guide.setTitle(request.getTitle());
        guide.setCoverImageUrl(request.getCoverImage());
        guide.setCategory(request.getCategory());
        guide.setContent(request.getContent());
        guide.setIsPublished(Boolean.TRUE.equals(request.getPublished()) ? 1 : 0);
        guideMapper.updateById(guide);

        markGuideSpotsDeleted(guideId);
        saveGuideSpots(guideId, request.getSpotIds());
        log.info("攻略更新成功: guideId={}, title={}", guideId, request.getTitle());
    }

    @Override
    public void updatePublishStatus(Long guideId, Boolean published) {
        Guide guide = getExistingGuide(guideId);
        guide.setIsPublished(Boolean.TRUE.equals(published) ? 1 : 0);
        guideMapper.updateById(guide);
        log.info("攻略发布状态变更: guideId={}, published={}", guideId, published);
    }

    @Override
    @Transactional
    public void deleteGuide(Long guideId) {
        Guide guide = getExistingGuide(guideId);
        guide.setIsDeleted(1);
        guideMapper.updateById(guide);

        markGuideSpotsDeleted(guideId);
        log.info("攻略已删除: guideId={}, title={}", guideId, guide.getTitle());
    }

    private Guide getExistingGuide(Long guideId) {
        Guide guide = guideMapper.selectById(guideId);
        if (guide == null || guide.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.GUIDE_NOT_FOUND);
        }
        return guide;
    }

    private AdminGuideListResponse convertToAdminListResponse(Guide guide) {
        return AdminGuideListResponse.builder()
            .id(guide.getId())
            .title(guide.getTitle())
            .coverImage(guide.getCoverImageUrl())
            .category(guide.getCategory())
            .viewCount(guide.getViewCount())
            .published(guide.getIsPublished() == 1)
            .createdAt(guide.getCreatedAt())
            .updatedAt(guide.getUpdatedAt())
            .build();
    }

    private AdminGuideRequest.SpotOption convertToSpotOption(Spot spot) {
        AdminGuideRequest.SpotOption option = new AdminGuideRequest.SpotOption();
        option.setId(spot.getId());
        option.setName(spot.getName());
        option.setPublished(spot.getIsPublished());
        option.setIsDeleted(spot.getIsDeleted());
        return option;
    }

    /**
     * 先将旧关联整体标记删除，再按当前请求重建有效关联，避免残留旧排序和脏数据。
     */
    private void markGuideSpotsDeleted(Long guideId) {
        GuideSpotRelation deletedSpot = new GuideSpotRelation();
        deletedSpot.setIsDeleted(1);
        guideSpotRelationMapper.update(
            deletedSpot,
            new LambdaQueryWrapper<GuideSpotRelation>().eq(GuideSpotRelation::getGuideId, guideId)
        );
    }

    private void saveGuideSpots(Long guideId, List<Long> spotIds) {
        if (spotIds == null || spotIds.isEmpty()) {
            return;
        }

        List<Long> uniqueSpotIds = spotIds.stream().distinct().collect(Collectors.toList());
        List<GuideSpotRelation> existingSpots = guideSpotRelationMapper.selectList(
            new LambdaQueryWrapper<GuideSpotRelation>()
                .eq(GuideSpotRelation::getGuideId, guideId)
                .in(GuideSpotRelation::getSpotId, uniqueSpotIds)
        );
        HashSet<Long> existingSpotIds = existingSpots.stream()
            .map(GuideSpotRelation::getSpotId)
            .collect(Collectors.toCollection(HashSet::new));

        for (int i = 0; i < uniqueSpotIds.size(); i++) {
            Long spotId = uniqueSpotIds.get(i);
            UpdateWrapper<GuideSpotRelation> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("guide_id", guideId)
                .eq("spot_id", spotId)
                .set("is_deleted", 0)
                .set("sort_order", i + 1)
                .set("updated_at", LocalDateTime.now());
            guideSpotRelationMapper.update(null, updateWrapper);
        }

        for (int i = 0; i < uniqueSpotIds.size(); i++) {
            Long spotId = uniqueSpotIds.get(i);
            if (existingSpotIds.contains(spotId)) {
                continue;
            }
            GuideSpotRelation guideSpot = new GuideSpotRelation();
            guideSpot.setGuideId(guideId);
            guideSpot.setSpotId(spotId);
            guideSpot.setSortOrder(i + 1);
            guideSpot.setIsDeleted(0);
            guideSpotRelationMapper.insert(guideSpot);
        }
    }
}
