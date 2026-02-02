package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 攻略关联景点实体
 */
@Data
@TableName("guide_spot_relation")
public class GuideSpot {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long guideId;

    private Long spotId;

    private Integer sortOrder;

    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
