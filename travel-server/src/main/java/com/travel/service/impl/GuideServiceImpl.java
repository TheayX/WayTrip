package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.PageResult;
import com.travel.common.result.ResultCode;
import com.travel.dto.guide.*;
import com.travel.entity.Guide;
import com.travel.entity.GuideSpotRelation;
import com.travel.entity.Spot;
import com.travel.mapper.GuideMapper;
import com.travel.mapper.GuideSpotRelationMapper;
import com.travel.mapper.SpotMapper;
import com.travel.service.GuideService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 攻略服务实现
 */
@Service
@RequiredArgsConstructor
public class GuideServiceImpl implements GuideService {

    private final GuideMapper guideMapper;
    private final GuideSpotRelationMapper guideSpotRelationMapper;
    private final SpotMapper spotMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public PageResult<GuideListResponse> getGuideList(GuideListRequest request) {
        Page<Guide> page = new Page<>(request.getPage(), request.getPageSize());

        LambdaQueryWrapper<Guide> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Guide::getIsPublished, 1);
        wrapper.eq(Guide::getIsDeleted, 0);

        if (StringUtils.hasText(request.getCategory())) {
            wrapper.eq(Guide::getCategory, request.getCategory());
        }

        // 排序
        if ("category".equals(request.getSortBy())) {
            wrapper.orderByAsc(Guide::getCategory).orderByDesc(Guide::getCreatedAt);
        } else {
            wrapper.orderByDesc(Guide::getCreatedAt);
        }

        Page<Guide> result = guideMapper.selectPage(page, wrapper);

        List<GuideListResponse> list = result.getRecords().stream()
                .map(this::convertToListResponse)
                .collect(Collectors.toList());

        return PageResult.of(list, result.getTotal(), request.getPage(), request.getPageSize());
    }

    @Override
    public GuideDetailResponse getGuideDetail(Long guideId) {
        Guide guide = guideMapper.selectById(guideId);
        if (guide == null || guide.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.GUIDE_NOT_FOUND);
        }
        if (guide.getIsPublished() != 1) {
            throw new BusinessException(ResultCode.GUIDE_OFFLINE);
        }

        // 增加浏览量（不更新 updatedAt）
        int nextViewCount = (guide.getViewCount() == null ? 0 : guide.getViewCount()) + 1;
        guide.setViewCount(nextViewCount);
        guideMapper.update(
                null,
                new UpdateWrapper<Guide>()
                        .eq("id", guideId)
                        .setSql("view_count = view_count + 1"));

        // 获取关联景点
        List<GuideDetailResponse.RelatedSpot> relatedSpots = getRelatedSpots(guideId);

        return GuideDetailResponse.builder()
                .id(guide.getId())
                .title(guide.getTitle())
                .coverImage(guide.getCoverImageUrl())
                .category(guide.getCategory())
                .content(guide.getContent())
                .viewCount(guide.getViewCount())
                .createdAt(guide.getCreatedAt().format(DATE_FORMATTER))
                .relatedSpots(relatedSpots)
                .build();
    }

    @Override
    public List<String> getCategories() {
        return guideMapper.selectDistinctCategories();
    }

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
        Guide guide = guideMapper.selectById(guideId);
        if (guide == null || guide.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.GUIDE_NOT_FOUND);
        }

        // 获取关联景点ID
        List<GuideSpotRelation> guideSpots = guideSpotRelationMapper.selectList(
                new LambdaQueryWrapper<GuideSpotRelation>()
                        .eq(GuideSpotRelation::getGuideId, guideId)
                        .eq(GuideSpotRelation::getIsDeleted, 0)
                        .orderByAsc(GuideSpotRelation::getSortOrder)
                        .orderByAsc(GuideSpotRelation::getId));
        List<Long> spotIds = guideSpots.stream()
                .map(GuideSpotRelation::getSpotId)
                .collect(Collectors.toList());

        List<AdminGuideRequest.SpotOption> spotOptions = new ArrayList<>();
        if (!spotIds.isEmpty()) {
            spotOptions = spotMapper.selectBatchIds(spotIds).stream()
                    .map(spot -> {
                        AdminGuideRequest.SpotOption option = new AdminGuideRequest.SpotOption();
                        option.setId(spot.getId());
                        option.setName(spot.getName());
                        option.setPublished(spot.getIsPublished());
                        option.setIsDeleted(spot.getIsDeleted());
                        return option;
                    })
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

        // 保存关联景点
        saveGuideSpots(guide.getId(), request.getSpotIds());

        return guide.getId();
    }

    @Override
    @Transactional
    public void updateGuide(Long guideId, AdminGuideRequest request) {
        Guide guide = guideMapper.selectById(guideId);
        if (guide == null || guide.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.GUIDE_NOT_FOUND);
        }

        guide.setTitle(request.getTitle());
        guide.setCoverImageUrl(request.getCoverImage());
        guide.setCategory(request.getCategory());
        guide.setContent(request.getContent());
        guide.setIsPublished(Boolean.TRUE.equals(request.getPublished()) ? 1 : 0);
        guideMapper.updateById(guide);

        // 更新关联景点
        GuideSpotRelation deletedSpot = new GuideSpotRelation();
        deletedSpot.setIsDeleted(1);
        guideSpotRelationMapper.update(
                deletedSpot,
                new LambdaQueryWrapper<GuideSpotRelation>().eq(GuideSpotRelation::getGuideId, guideId));
        saveGuideSpots(guideId, request.getSpotIds());
    }

    @Override
    public void updatePublishStatus(Long guideId, Boolean published) {
        Guide guide = guideMapper.selectById(guideId);
        if (guide == null || guide.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.GUIDE_NOT_FOUND);
        }
        guide.setIsPublished(published ? 1 : 0);
        guideMapper.updateById(guide);
    }

    @Override
    @Transactional
    public void deleteGuide(Long guideId) {
        Guide guide = guideMapper.selectById(guideId);
        if (guide == null || guide.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.GUIDE_NOT_FOUND);
        }
        guide.setIsDeleted(1);
        guideMapper.updateById(guide);

        GuideSpotRelation deletedSpot = new GuideSpotRelation();
        deletedSpot.setIsDeleted(1);
        guideSpotRelationMapper.update(
                deletedSpot,
                new LambdaQueryWrapper<GuideSpotRelation>().eq(GuideSpotRelation::getGuideId, guideId));
    }

    private GuideListResponse convertToListResponse(Guide guide) {
        String summary = guide.getContent();
        if (summary != null && summary.length() > 100) {
            // 去除HTML标签并截取摘要
            summary = summary.replaceAll("<[^>]+>", "");
            if (summary.length() > 100) {
                summary = summary.substring(0, 100) + "...";
            }
        }

        return GuideListResponse.builder()
                .id(guide.getId())
                .title(guide.getTitle())
                .coverImage(guide.getCoverImageUrl())
                .category(guide.getCategory())
                .summary(summary)
                .viewCount(guide.getViewCount())
                .createdAt(guide.getCreatedAt().format(DATE_FORMATTER))
                .build();
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


    private List<GuideDetailResponse.RelatedSpot> getRelatedSpots(Long guideId) {
        List<GuideSpotRelation> guideSpots = guideSpotRelationMapper.selectList(
                new LambdaQueryWrapper<GuideSpotRelation>()
                        .eq(GuideSpotRelation::getGuideId, guideId)
                        .eq(GuideSpotRelation::getIsDeleted, 0)
                        .orderByAsc(GuideSpotRelation::getSortOrder)
                        .orderByAsc(GuideSpotRelation::getId));

        if (guideSpots.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> spotIds = guideSpots.stream()
                .map(GuideSpotRelation::getSpotId)
                .collect(Collectors.toList());

        List<Spot> spots = spotMapper.selectBatchIds(spotIds);

        return spots.stream()
                .filter(s -> s.getIsDeleted() == 0 && s.getIsPublished() == 1)
                .map(s -> GuideDetailResponse.RelatedSpot.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .coverImage(s.getCoverImageUrl())
                        .price("¥" + s.getPrice())
                        .build())
                .collect(Collectors.toList());
    }

    private void saveGuideSpots(Long guideId, List<Long> spotIds) {
        if (spotIds == null || spotIds.isEmpty()) {
            return;
        }

        List<Long> uniqueSpotIds = spotIds.stream()
                .distinct()
                .collect(Collectors.toList());

        List<GuideSpotRelation> existingSpots = guideSpotRelationMapper.selectList(
                new LambdaQueryWrapper<GuideSpotRelation>()
                        .eq(GuideSpotRelation::getGuideId, guideId)
                        .in(GuideSpotRelation::getSpotId, uniqueSpotIds));
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
