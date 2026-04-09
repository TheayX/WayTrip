package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 攻略与景点关联持久化实体，对应 guide_spot_relation 表。
 * <p>
 * 用于维护攻略与景点的关联关系以及前端展示顺序。
 */
@Data
@TableName("guide_spot_relation")
public class GuideSpotRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long guideId;

    private Long spotId;

    private Integer sortOrder;

    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

