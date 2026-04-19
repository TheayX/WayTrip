package com.travel.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 知识文档实体，对应 ai_knowledge_document 表。
 */
@Data
@TableName("ai_knowledge_document")
public class AiKnowledgeDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String knowledgeDomain;

    private String sourceType;

    private String sourceRef;

    private String content;

    private String tags;

    private Integer version;

    private String indexStatus;

    private Integer retryCount;

    private String lastError;

    private LocalDateTime rebuildRequestedAt;

    private LocalDateTime rebuildStartedAt;

    private LocalDateTime rebuildFinishedAt;

    private Integer isEnabled;

    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
