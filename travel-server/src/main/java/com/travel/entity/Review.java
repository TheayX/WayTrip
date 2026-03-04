package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价实体（对应 user_spot_review 表）
 */
@Data
@TableName("user_spot_review")
public class Review {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long spotId;

    private Integer score;

    private String comment;

    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // 非数据库字段
    @TableField(exist = false)
    private String nickname;

    @TableField(exist = false)
    private String avatarUrl;
}

