package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.PageResult;
import com.travel.common.result.ResultCode;
import com.travel.dto.guide.*;
import com.travel.entity.Guide;
import com.travel.entity.GuideSpot;
import com.travel.entity.Spot;
import com.travel.mapper.GuideMapper;
import com.travel.mapper.GuideSpotMapper;
import com.travel.mapper.SpotMapper;
import com.travel.service.GuideService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final GuideSpotMapper guideSpotMapper;
    private final SpotMapper spotMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public PageResult<GuideListResponse> getGuideList(GuideListRequest request) {
        Page<Guide> page = new Page<>(request.getPage(), request.getPageSize());

        LambdaQueryWrapper<Guide> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Guide::getPublished, 1);
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
        if (guide.getPublished() != 1) {
            throw new BusinessException(ResultCode.GUIDE_OFFLINE);
        }

        // 增加浏览量
        guide.setViewCount(guide.getViewCount() + 1);
        guideMapper.updateById(guide);

        // 获取关联景点
        List<GuideDetailResponse.RelatedSpot> relatedSpots = getRelatedSpots(guideId);

        return GuideDetailResponse.builder()
                .id(guide.getId())
                .title(guide.getTitle())
                .coverImage(guide.getCoverImage())
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
            wrapper.eq(Guide::getPublished, request.getPublished());
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
        List<GuideSpot> guideSpots = guideSpotMapper.selectList(
                new LambdaQueryWrapper<GuideSpot>()
                        .eq(GuideSpot::getGuideId, guideId)
                        .eq(GuideSpot::getIsDeleted, 0));
        List<Long> spotIds = guideSpots.stream()
                .map(GuideSpot::getSpotId)
                .collect(Collectors.toList());

        AdminGuideRequest response = new AdminGuideRequest();
        response.setTitle(guide.getTitle());
        response.setCoverImage(guide.getCoverImage());
        response.setCategory(guide.getCategory());
        response.setContent(guide.getContent());
        response.setPublished(guide.getPublished() == 1);
        response.setSpotIds(spotIds);

        return response;
    }

    @Override
    @Transactional
    public Long createGuide(AdminGuideRequest request, Long adminId) {
        Guide guide = new Guide();
        guide.setTitle(request.getTitle());
        guide.setCoverImage(request.getCoverImage());
        guide.setCategory(request.getCategory());
        guide.setContent(request.getContent());
        guide.setAdminId(adminId);
        guide.setPublished(Boolean.TRUE.equals(request.getPublished()) ? 1 : 0);
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
        guide.setCoverImage(request.getCoverImage());
        guide.setCategory(request.getCategory());
        guide.setContent(request.getContent());
        guide.setPublished(Boolean.TRUE.equals(request.getPublished()) ? 1 : 0);
        guideMapper.updateById(guide);

        // 更新关联景点
        GuideSpot deletedSpot = new GuideSpot();
        deletedSpot.setIsDeleted(1);
        guideSpotMapper.update(
                deletedSpot,
                new LambdaQueryWrapper<GuideSpot>().eq(GuideSpot::getGuideId, guideId));
        saveGuideSpots(guideId, request.getSpotIds());
    }

    @Override
    public void updatePublishStatus(Long guideId, Boolean published) {
        Guide guide = guideMapper.selectById(guideId);
        if (guide == null || guide.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.GUIDE_NOT_FOUND);
        }
        guide.setPublished(published ? 1 : 0);
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

        GuideSpot deletedSpot = new GuideSpot();
        deletedSpot.setIsDeleted(1);
        guideSpotMapper.update(
                deletedSpot,
                new LambdaQueryWrapper<GuideSpot>().eq(GuideSpot::getGuideId, guideId));
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
                .coverImage(guide.getCoverImage())
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
                .coverImage(guide.getCoverImage())
                .category(guide.getCategory())
                .viewCount(guide.getViewCount())
                .published(guide.getPublished() == 1)
                .createdAt(guide.getCreatedAt())
                .build();
    }

    private List<GuideDetailResponse.RelatedSpot> getRelatedSpots(Long guideId) {
        List<GuideSpot> guideSpots = guideSpotMapper.selectList(
                new LambdaQueryWrapper<GuideSpot>()
                        .eq(GuideSpot::getGuideId, guideId)
                        .eq(GuideSpot::getIsDeleted, 0));

        if (guideSpots.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> spotIds = guideSpots.stream()
                .map(GuideSpot::getSpotId)
                .collect(Collectors.toList());

        List<Spot> spots = spotMapper.selectBatchIds(spotIds);

        return spots.stream()
                .filter(s -> s.getIsDeleted() == 0 && s.getPublished() == 1)
                .map(s -> GuideDetailResponse.RelatedSpot.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .coverImage(s.getCoverImage())
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

        List<GuideSpot> existingSpots = guideSpotMapper.selectList(
                new LambdaQueryWrapper<GuideSpot>()
                        .eq(GuideSpot::getGuideId, guideId)
                        .in(GuideSpot::getSpotId, uniqueSpotIds));
        HashSet<Long> existingSpotIds = existingSpots.stream()
                .map(GuideSpot::getSpotId)
                .collect(Collectors.toCollection(HashSet::new));

        for (GuideSpot existingSpot : existingSpots) {
            if (existingSpot.getIsDeleted() != null && existingSpot.getIsDeleted() == 1) {
                GuideSpot toUpdate = new GuideSpot();
                toUpdate.setIsDeleted(0);
                guideSpotMapper.update(
                        toUpdate,
                        new LambdaQueryWrapper<GuideSpot>()
                                .eq(GuideSpot::getGuideId, guideId)
                                .eq(GuideSpot::getSpotId, existingSpot.getSpotId()));
            }
        }

        for (Long spotId : uniqueSpotIds) {
            if (existingSpotIds.contains(spotId)) {
                continue;
            }
            GuideSpot guideSpot = new GuideSpot();
            guideSpot.setGuideId(guideId);
            guideSpot.setSpotId(spotId);
            guideSpot.setIsDeleted(0);
            guideSpotMapper.insert(guideSpot);
        }
    }
}
