package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.PageResult;
import com.travel.common.result.ResultCode;
import com.travel.dto.guide.model.GuideBudgetQueryResult;
import com.travel.dto.guide.request.GuideBudgetListRequest;
import com.travel.dto.guide.request.GuideListRequest;
import com.travel.dto.guide.response.GuideBudgetListResponse;
import com.travel.dto.guide.response.GuideDetailResponse;
import com.travel.dto.guide.response.GuideListResponse;
import com.travel.entity.Guide;
import com.travel.entity.GuideSpotRelation;
import com.travel.entity.Spot;
import com.travel.mapper.GuideMapper;
import com.travel.mapper.GuideSpotRelationMapper;
import com.travel.mapper.SpotMapper;
import com.travel.service.GuideQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 攻略查询服务实现，负责用户端列表、详情和分类查询。
 */
@Service
@RequiredArgsConstructor
public class GuideQueryServiceImpl implements GuideQueryService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final GuideMapper guideMapper;
    private final GuideSpotRelationMapper guideSpotRelationMapper;
    private final SpotMapper spotMapper;

    @Override
    public PageResult<GuideListResponse> getGuideList(GuideListRequest request) {
        Page<Guide> page = new Page<>(request.getPage(), request.getPageSize());
        LambdaQueryWrapper<Guide> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Guide::getIsPublished, 1);
        wrapper.eq(Guide::getIsDeleted, 0);

        if (StringUtils.hasText(request.getCategory())) {
            wrapper.eq(Guide::getCategory, request.getCategory());
        }

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
            request.getMaxPrice()
        );

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

        int nextViewCount = (guide.getViewCount() == null ? 0 : guide.getViewCount()) + 1;
        guide.setViewCount(nextViewCount);
        guideMapper.update(
            null,
            new UpdateWrapper<Guide>()
                .eq("id", guideId)
                .setSql("view_count = view_count + 1")
        );

        return GuideDetailResponse.builder()
            .id(guide.getId())
            .title(guide.getTitle())
            .coverImage(guide.getCoverImageUrl())
            .category(guide.getCategory())
            .content(guide.getContent())
            .viewCount(guide.getViewCount())
            .createdAt(guide.getCreatedAt().format(DATE_FORMATTER))
            .relatedSpots(getRelatedSpots(guideId))
            .build();
    }

    @Override
    public List<String> getCategories() {
        return guideMapper.selectDistinctCategories();
    }

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

    private GuideBudgetListResponse convertToBudgetListResponse(GuideBudgetQueryResult guide) {
        String priceLabel = guide.getMinPrice() != null && guide.getMinPrice().compareTo(BigDecimal.ZERO) == 0
            ? "含免费景点"
            : String.format("%s 元起", guide.getMinPrice() == null ? 0 : guide.getMinPrice().stripTrailingZeros().toPlainString());

        return GuideBudgetListResponse.builder()
            .id(guide.getId())
            .title(guide.getTitle())
            .coverImage(guide.getCoverImage())
            .category(guide.getCategory())
            .summary(buildGuideSummary(guide.getContent()))
            .viewCount(guide.getViewCount())
            .createdAt(guide.getCreatedAt() != null ? guide.getCreatedAt().format(DATE_FORMATTER) : null)
            .relatedCount(guide.getRelatedCount())
            .priceLabel(priceLabel)
            .build();
    }

    private List<GuideDetailResponse.RelatedSpot> getRelatedSpots(Long guideId) {
        List<GuideSpotRelation> guideSpots = guideSpotRelationMapper.selectList(
            new LambdaQueryWrapper<GuideSpotRelation>()
                .eq(GuideSpotRelation::getGuideId, guideId)
                .eq(GuideSpotRelation::getIsDeleted, 0)
                .orderByAsc(GuideSpotRelation::getSortOrder)
                .orderByAsc(GuideSpotRelation::getId)
        );

        if (guideSpots.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> spotIds = guideSpots.stream()
            .map(GuideSpotRelation::getSpotId)
            .collect(Collectors.toList());
        List<Spot> spots = spotMapper.selectBatchIds(spotIds);

        return spots.stream()
            .filter(spot -> spot.getIsDeleted() == 0 && spot.getIsPublished() == 1)
            .map(spot -> GuideDetailResponse.RelatedSpot.builder()
                .id(spot.getId())
                .name(spot.getName())
                .coverImage(spot.getCoverImageUrl())
                .price("￥" + spot.getPrice())
                .build())
            .collect(Collectors.toList());
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
