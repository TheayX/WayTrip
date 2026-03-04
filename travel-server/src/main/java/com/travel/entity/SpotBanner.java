package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 景点轮播图实体（对应 spot_banner 表）
 */
@Data
@TableName("spot_banner")
public class SpotBanner {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String imageUrl;

    private Long spotId;

    private Integer sortOrder;

    @TableField("is_enabled")
    private Integer isEnabled;

    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // 非数据库字段
    @TableField(exist = false)
    private String spotName;
}

