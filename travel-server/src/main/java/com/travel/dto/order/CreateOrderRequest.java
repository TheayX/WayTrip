package com.travel.dto.order;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建订单请求
 */
@Data
public class CreateOrderRequest {

    @NotNull(message = "景点ID不能为空")
    private Long spotId;

    @NotNull(message = "购票数量不能为空")
    @Min(value = 1, message = "购票数量至少为1")
    @Max(value = 99, message = "购票数量最多为99")
    private Integer quantity;

    @NotNull(message = "游玩日期不能为空")
    @Future(message = "游玩日期必须是将来的日期")
    private LocalDate visitDate;

    @NotBlank(message = "联系人姓名不能为空")
    @Size(max = 50, message = "联系人姓名不能超过50个字符")
    private String contactName;

    @NotBlank(message = "联系人电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String contactPhone;

    /**
     * 幂等键，用于防止重复提交
     */
    private String idempotentKey;
}
