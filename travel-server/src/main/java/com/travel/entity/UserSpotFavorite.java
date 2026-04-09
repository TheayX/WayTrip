package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户景点收藏持久化实体，对应 user_spot_favorite 表。
 * <p>
 * 收藏记录使用软删除管理，便于取消收藏后仍保留历史关系。
 */
@Data
@TableName("user_spot_favorite")
public class UserSpotFavorite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long spotId;

    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

