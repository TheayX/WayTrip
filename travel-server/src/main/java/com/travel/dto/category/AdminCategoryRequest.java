package com.travel.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理端分类创建或更新请求对象。
 */
@Data
@Schema(description = "管理端分类请求参数")
public class AdminCategoryRequest {

    @Schema(description = "父分类ID (0表示顶级分类)", example = "0")
    @NotNull(message = "父分类ID不能为空")
    private Long parentId;

    @Schema(description = "分类名称", example = "周边游")
    @NotBlank(message = "分类名称不能为空")
    private String name;

    @Schema(description = "分类图标URL", example = "http://xxx/icon.png")
    private String iconUrl;

    @Schema(description = "排序序号", example = "1")
    private Integer sortOrder;
}
