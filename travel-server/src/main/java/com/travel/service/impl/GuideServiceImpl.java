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
import lombok.extern.slf4j.Slf4j;
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
 * 攻略服务实现，负责用户端浏览、管理端维护与景点关联编排。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuideServiceImpl implements GuideService {

    // 持久层依赖
    private final GuideMapper guideMapper;
    private final GuideSpotRelationMapper guideSpotRelationMapper;
    private final SpotMapper spotMapper;

    // 时间格式配置
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 用户端攻略浏览

    @Override
    public PageResult<GuideListResponse> getGuideList(GuideListRequest request) {
        Page<Guide> page = new Page<>(request.getPage(), request.getPageSize());

        LambdaQueryWrapper<Guide> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Guide::getIsPublished, 1);
        wrapper.eq(Guide::getIsDeleted, 0);

        if (StringUtils.hasText(request.getCategory())) {
            wrapper.eq(Guide::getCategory, request.getCategory());
        }

        // 默认按发布时间倒序，分类排序仅作为后台筛查场景的补充。
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
    public PageResult<GuideBudgetListResponse> getBudgetGuideList(GuideBudgetListRequest request) {
        Page<GuideBudgetQueryResult> page = new Page<>(request.getPage(), request.getPageSize());
        Page<GuideBudgetQueryResult> result = (Page<GuideBudgetQueryResult>) guideMapper.selectBudgetGuidePage(
                page,
                request.getPriceMode(),
                request.getMaxPrice());

        List<GuideBudgetListResponse> list = result.getRecords().stream()
                .map(this::convertToBudgetListResponse)
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

        // 浏览量单独累加，不改动 updatedAt，避免干扰后台编辑时间语义。
        int nextViewCount = (guide.getViewCount() == null ? 0 : guide.getViewCount()) + 1;
        guide.setViewCount(nextViewCount);
        guideMapper.update(
                null,
                new UpdateWrapper<Guide>()
                        .eq("id", guideId)
                        .setSql("view_count = view_count + 1"));

        // 详情页返回攻略关联景点摘要，用于引导继续浏览。
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

    // 管理端攻略维护

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

        // 编辑回显需要保留已下架或已删除景点的信息，避免历史关联丢失。
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

        // 关联景点单独存表，创建主记录后再批量写入关联关系。
        saveGuideSpots(guide.getId(), request.getSpotIds());

        log.info("攻略创建成功: guideId={}, title={}, adminId={}", guide.getId(), guide.getTitle(), adminId);

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

        // 关联关系按“先软删再重建”处理，避免排序和删除状态残留。
        GuideSpotRelation deletedSpot = new GuideSpotRelation();
        deletedSpot.setIsDeleted(1);
        guideSpotRelationMapper.update(
                deletedSpot,
                new LambdaQueryWrapper<GuideSpotRelation>().eq(GuideSpotRelation::getGuideId, guideId));
        saveGuideSpots(guideId, request.getSpotIds());

        log.info("攻略更新成功: guideId={}, title={}", guideId, request.getTitle());
    }

    @Override
    public void updatePublishStatus(Long guideId, Boolean published) {
        Guide guide = guideMapper.selectById(guideId);
        if (guide == null || guide.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.GUIDE_NOT_FOUND);
        }
        guide.setIsPublished(published ? 1 : 0);
        guideMapper.updateById(guide);
        log.info("攻略发布状态变更: guideId={}, published={}", guideId, published);
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

        log.info("攻略已删除: guideId={}, title={}", guideId, guide.getTitle());
    }

    // 响应对象转换方法
    // 响应转换与关联景点组装

    private GuideListResponse convertToListResponse(Guide guide) {
        return GuideListResponse.builder()
                .id(guide.getId())
                .title(guide.getTitle())
                .coverImage(guide.getCoverImageUrl())
                .category(guide.getCategory())
                .summary(buildGuideSummary(guide.getContent()))
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

    private GuideBudgetListResponse convertToBudgetListResponse(GuideBudgetQueryResult guide) {
        String summary = buildGuideSummary(guide.getContent());
        String priceLabel = guide.getMinPrice() != null && guide.getMinPrice().compareTo(java.math.BigDecimal.ZERO) == 0
                ? "含免费景点"
                : String.format("%s 元起", guide.getMinPrice() == null ? 0 : guide.getMinPrice().stripTrailingZeros().toPlainString());

        return GuideBudgetListResponse.builder()
                .id(guide.getId())
                .title(guide.getTitle())
                .coverImage(guide.getCoverImage())
                .category(guide.getCategory())
                .summary(summary)
                .viewCount(guide.getViewCount())
                .createdAt(guide.getCreatedAt() != null ? guide.getCreatedAt().format(DATE_FORMATTER) : null)
                .relatedCount(guide.getRelatedCount())
                .priceLabel(priceLabel)
                .build();
    }

    // 攻略关联景点处理方法
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

    private String buildGuideSummary(String content) {
        if (content == null) {
            return null;
        }
        String summary = content.replaceAll("<[^>]+>", "");
        if (summary.length() > 100) {
            return summary.substring(0, 100) + "...";
        }
        return summary;
    }
}
