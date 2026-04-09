package com.travel.dto.region.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理端地区创建或更新请求对象。
 * <p>
 * 后台地区树新增和编辑共用同一请求结构，保持地区基础数据维护一致。
 */
@Data
@Schema(description = "管理端地区请求参数")
public class AdminRegionRequest {

    @Schema(description = "父地区ID (0表示顶级地区)", example = "0")
    @NotNull(message = "父地区ID不能为空")
    private Long parentId;

    @Schema(description = "地区名称", example = "北京")
    @NotBlank(message = "地区名称不能为空")
    private String name;

    @Schema(description = "排序序号", example = "1")
    private Integer sortOrder;
}
