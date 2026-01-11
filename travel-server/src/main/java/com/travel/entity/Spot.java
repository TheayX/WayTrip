package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 景点实体
 */
@Data
@TableName("spot")
public class Spot {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private BigDecimal price;

    private String openTime;

    private String address;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private String coverImage;

    private Long categoryId;

    private Long regionId;

    private Integer heatScore;

    private BigDecimal avgRating;

    private Integer ratingCount;

    private Integer published;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // 非数据库字段
    @TableField(exist = false)
    private String categoryName;

    @TableField(exist = false)
    private String regionName;
}
