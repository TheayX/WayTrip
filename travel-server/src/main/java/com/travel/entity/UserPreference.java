package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户偏好标签实体
 */
@Data
@TableName("user_preference")
public class UserPreference {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String tag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
