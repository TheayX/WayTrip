package com.travel.dto.ai.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 回复引用项。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiCitationItem {

    private String title;
    private String sourceType;
    private String sourceRef;
    private String snippet;
}
