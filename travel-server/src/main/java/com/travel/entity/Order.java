package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单实体 - 与数据库order表完全对应
 * status: 0-待支付，1-已支付，2-已取消，3-已退款
 */
@Data
@TableName("`order`")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private Long spotId;

    private Integer quantity;

    private BigDecimal totalAmount;

    /**
     * 订单状态：0-待支付，1-已支付，2-已取消，3-已退款
     */
    private Integer status;

    private LocalDate visitDate;

    private String contactName;

    private String contactPhone;

    private LocalDateTime paidAt;

    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ========== 非数据库字段，通过JOIN查询填充 ==========

    @TableField(exist = false)
    private String spotName;

    @TableField(exist = false)
    private String spotImage;

    @TableField(exist = false)
    private BigDecimal unitPrice;

    // 状态常量
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_PAID = 1;
    public static final int STATUS_CANCELLED = 2;
    public static final int STATUS_REFUNDED = 3;
}
