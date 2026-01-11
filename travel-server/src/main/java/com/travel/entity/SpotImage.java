package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 景点图片实体
 */
@Data
@TableName("spot_image")
public class SpotImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long spotId;

    private String imageUrl;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
