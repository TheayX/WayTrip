package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travel.common.exception.BusinessException;
import com.travel.dto.region.AdminRegionRequest;
import com.travel.dto.region.AdminRegionResponse;
import com.travel.entity.SpotRegion;
import com.travel.mapper.SpotRegionMapper;
import com.travel.service.SpotRegionService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SpotRegionServiceImpl extends ServiceImpl<SpotRegionMapper, SpotRegion> implements SpotRegionService {

    @Override
    public List<AdminRegionResponse> getAdminRegions(Long parentId) {
        LambdaQueryWrapper<SpotRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SpotRegion::getIsDeleted, 0);
        if (parentId != null) {
            wrapper.eq(SpotRegion::getParentId, parentId);
        }
        wrapper.orderByAsc(SpotRegion::getSortOrder)
               .orderByDesc(SpotRegion::getCreatedAt);
               
        List<SpotRegion> regions = list(wrapper);
        return regions.stream().map(region -> {
            AdminRegionResponse response = new AdminRegionResponse();
            BeanUtils.copyProperties(Objects.requireNonNull(region), response);
            return response;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRegion(AdminRegionRequest request) {
        SpotRegion region = new SpotRegion();
        BeanUtils.copyProperties(Objects.requireNonNull(request), region);
        region.setIsDeleted(0);
        save(region);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRegion(Long id, AdminRegionRequest request) {
        SpotRegion region = getById(id);
        if (region == null || region.getIsDeleted() == 1) {
            throw new BusinessException(404, "地区不存在");
        }
        BeanUtils.copyProperties(Objects.requireNonNull(request), region);
        region.setId(id);
        updateById(region);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRegion(Long id) {
        SpotRegion region = getById(id);
        if (region == null || region.getIsDeleted() == 1) {
            return;
        }
        
        // 检查是否有子地区
        LambdaQueryWrapper<SpotRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SpotRegion::getParentId, id)
               .eq(SpotRegion::getIsDeleted, 0);
        if (count(wrapper) > 0) {
            throw new BusinessException(400, "该地区下还存在子地区，请先删除子地区");
        }
        
        region.setIsDeleted(1);
        updateById(region);
    }
}
