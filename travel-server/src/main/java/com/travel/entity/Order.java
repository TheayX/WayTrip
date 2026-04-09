package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单持久化实体，对应 order 表。
 * <p>
 * 订单实体既承接用户下单主链路，也为后台状态流转和统计提供基础数据。
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
     * 订单状态：0-待支付，1-已支付，2-已取消，3-已退款，4-已完成
     */
    private Integer status;

    private LocalDate visitDate;

    private String contactName;

    private String contactPhone;

    private LocalDateTime paidAt;

    private LocalDateTime cancelledAt;

    private LocalDateTime refundedAt;

    private LocalDateTime completedAt;

    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 非数据库字段，通过关联查询补充。
     */

    @TableField(exist = false)
    private String spotName;

    @TableField(exist = false)
    private String spotImage;

    @TableField(exist = false)
    private BigDecimal unitPrice;

}
