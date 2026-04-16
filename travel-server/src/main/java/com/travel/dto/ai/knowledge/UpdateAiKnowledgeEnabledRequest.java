package com.travel.dto.ai.knowledge;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI 知识文档启用状态更新请求。
 */
@Data
public class UpdateAiKnowledgeEnabledRequest {

    @NotNull(message = "启用状态不能为空")
    @Min(value = 0, message = "启用状态只能为 0 或 1")
    @Max(value = 1, message = "启用状态只能为 0 或 1")
    private Integer isEnabled;
}
