package com.travel.dto.region.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端地区响应对象。
 */
@Data
@Schema(description = "管理端地区响应参数")
public class AdminRegionResponse {

    @Schema(description = "地区ID", example = "1")
    private Long id;

    @Schema(description = "父地区ID", example = "0")
    private Long parentId;

    @Schema(description = "地区名称", example = "北京")
    private String name;

    @Schema(description = "排序序号", example = "1")
    private Integer sortOrder;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
