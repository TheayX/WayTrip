package com.travel.service.support.spot;

import com.travel.dto.spot.request.AdminSpotUpsertRequest;
import com.travel.entity.Spot;
import com.travel.entity.SpotImage;
import com.travel.mapper.SpotImageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 景点写入支撑，集中处理后台写入字段映射和图片保存。
 * <p>
 * 景点表单到实体的映射细节统一收口，便于创建和更新共享同一套字段口径。
 */
@Component
@RequiredArgsConstructor
public class SpotWriteSupport {

    private final SpotImageMapper spotImageMapper;

    /**
     * 后台新增和更新共用同一套字段映射，避免写入口径在两处漂移。
     */
    public void copyUpsertRequest(AdminSpotUpsertRequest request, Spot spot) {
        if (request.getName() != null) {
            spot.setName(request.getName());
        }
        if (request.getDescription() != null) {
            spot.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            spot.setPrice(request.getPrice());
        }
        if (request.getOpenTime() != null) {
            spot.setOpenTime(request.getOpenTime());
        }
        if (request.getAddress() != null) {
            spot.setAddress(request.getAddress());
        }
        if (request.getLatitude() != null) {
            spot.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            spot.setLongitude(request.getLongitude());
        }
        if (request.getCoverImage() != null) {
            spot.setCoverImageUrl(request.getCoverImage());
        }
        if (request.getRegionId() != null) {
            spot.setRegionId(request.getRegionId());
        }
        if (request.getCategoryId() != null) {
            spot.setCategoryId(request.getCategoryId());
        }
        if (request.getPublished() != null) {
            spot.setIsPublished(Boolean.TRUE.equals(request.getPublished()) ? 1 : 0);
        }
        if (request.getHeatLevel() != null) {
            spot.setHeatLevel(request.getHeatLevel());
        }
    }

    /**
     * 图片写入统一按顺序落库，保证前端回显顺序和后台提交顺序一致。
     */
    public void saveSpotImages(Long spotId, List<String> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        for (int i = 0; i < images.size(); i++) {
            SpotImage image = new SpotImage();
            image.setSpotId(spotId);
            image.setImageUrl(images.get(i));
            image.setSortOrder(i + 1);
            spotImageMapper.insert(image);
        }
    }
}
