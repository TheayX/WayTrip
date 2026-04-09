package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 景点图片持久化实体，对应 spot_image 表。
 * <p>
 * 用于维护景点相册图片及其排序，支持后台整体替换图片集。
 */
@Data
@TableName("spot_image")
public class SpotImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long spotId;

    private String imageUrl;

    private Integer sortOrder;

    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
