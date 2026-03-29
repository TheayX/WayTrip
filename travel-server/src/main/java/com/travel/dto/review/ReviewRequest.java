package com.travel.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户端评价提交请求参数对象。
 */
@Data
public class ReviewRequest {
    @NotNull(message = "景点ID不能为空")
    private Long spotId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    private Integer score;

    private String comment;
}

