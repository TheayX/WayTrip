package com.travel.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "管理端分类响应参数")
public class AdminCategoryResponse {

    @Schema(description = "分类ID", example = "1")
    private Long id;

    @Schema(description = "父分类ID", example = "0")
    private Long parentId;

    @Schema(description = "分类名称", example = "周边游")
    private String name;

    @Schema(description = "分类图标URL", example = "http://xxx/icon.png")
    private String iconUrl;

    @Schema(description = "排序序号", example = "1")
    private Integer sortOrder;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
