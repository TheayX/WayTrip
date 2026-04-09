package com.travel.dto.guide.query;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 穷游攻略查询结果对象。
 * <p>
 * 作为 Mapper 联表聚合结果载体，承接穷游攻略查询中的价格与关联景点统计字段。
 */
@Data
public class GuideBudgetQueryResult {
    private Long id;
    private String title;
    private String coverImage;
    private String category;
    private String content;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private BigDecimal minPrice;
    private Integer relatedCount;
}
