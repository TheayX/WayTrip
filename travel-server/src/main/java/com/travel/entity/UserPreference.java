package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户偏好标签持久化实体，对应 user_preference 表。
 * <p>
 * 偏好标签会同时影响推荐冷启动和个人偏好展示，因此单独建表维护。
 */
@Data
@TableName("user_preference")
public class UserPreference {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String tag;

    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
