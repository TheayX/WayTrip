package com.travel.dto.ai.knowledge;

import com.travel.enums.ai.AiKnowledgeDomain;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 手工知识文档导入请求对象。
 */
@Data
public class ManualAiKnowledgeUpsertRequest {

    @NotBlank(message = "知识标题不能为空")
    private String title;

    @NotNull(message = "知识域不能为空")
    private AiKnowledgeDomain knowledgeDomain;

    @NotBlank(message = "知识内容不能为空")
    private String content;

    private String sourceType = "manual";

    private String sourceRef = "";

    private String tags = "";
}
