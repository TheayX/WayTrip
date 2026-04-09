package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 攻略持久化实体，对应 guide 表。
 * <p>
 * 攻略正文、分类、浏览量和发布状态都由该实体统一映射，支撑用户端浏览与后台管理。
 */
@Data
@TableName("guide")
public class Guide {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private String coverImageUrl;

    private String category;

    private Long adminId;

    private Integer viewCount;

    private Integer isPublished;

    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
