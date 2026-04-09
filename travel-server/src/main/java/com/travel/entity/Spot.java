package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 景点持久化实体，对应 spot 表。
 * <p>
 * 同时承接基础表字段和少量联表补充字段，是景点查询和后台管理的核心实体。
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

    private String coverImageUrl;

    private Long categoryId;

    private Long regionId;

    private Integer heatLevel;

    private Integer heatScore;

    private BigDecimal avgRating;

    private Integer ratingCount;

    private Integer isPublished;

    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 非数据库字段，通过关联查询补充。
     */
    @TableField(exist = false)
    private String categoryName;

    @TableField(exist = false)
    private String regionName;
}
