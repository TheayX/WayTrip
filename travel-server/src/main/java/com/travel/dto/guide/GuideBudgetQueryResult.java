package com.travel.dto.guide;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 穷游攻略查询结果对象。
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
