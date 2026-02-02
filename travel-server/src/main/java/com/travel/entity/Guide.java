package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 攻略实体
 */
@Data
@TableName("guide")
public class Guide {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    @TableField("cover_image_url")
    private String coverImage;

    private String category;

    private Long adminId;

    private Integer viewCount;

    @TableField("is_published")
    private Integer published;

    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
