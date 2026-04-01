package com.travel.service.support.recommendation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.entity.Spot;
import com.travel.entity.SpotCategory;
import com.travel.entity.SpotRegion;
import com.travel.mapper.SpotCategoryMapper;
import com.travel.mapper.SpotMapper;
import com.travel.mapper.SpotRegionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 推荐模块景点元信息支撑，集中处理名称和分类地区字典查询。
 */
@Component
@RequiredArgsConstructor
public class RecommendationMetadataSupport {

    private final SpotMapper spotMapper;
    private final SpotCategoryMapper categoryMapper;
    private final SpotRegionMapper spotRegionMapper;

    public Map<Long, String> getCategoryMap() {
        return categoryMapper.selectList(new LambdaQueryWrapper<SpotCategory>().eq(SpotCategory::getIsDeleted, 0)).stream()
            .collect(Collectors.toMap(SpotCategory::getId, SpotCategory::getName));
    }

    public Map<Long, String> getRegionMap() {
        return spotRegionMapper.selectList(new LambdaQueryWrapper<SpotRegion>().eq(SpotRegion::getIsDeleted, 0)).stream()
            .collect(Collectors.toMap(SpotRegion::getId, SpotRegion::getName));
    }

    public String getSpotName(Long spotId) {
        if (spotId == null) {
            return "未知景点";
        }
        Spot spot = spotMapper.selectById(spotId);
        return spot == null || spot.getName() == null ? "未知景点" : spot.getName();
    }
}
