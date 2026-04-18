package com.travel.service.ai.rag;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * AI 向量索引信息解析工具。
 * <p>
 * Redis FT.INFO 在不同客户端版本下返回结构可能存在差异，这里统一负责递归提取维度等公共字段。
 */
@Component
public class AiVectorIndexInfoSupport {

    /**
     * 从 FT.INFO 返回结果中递归提取向量维度。
     *
     * @param source FT.INFO 原始结果
     * @return 向量维度；无法解析时返回 {@code null}
     */
    public Integer extractDimension(Object source) {
        if (source instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if ("DIM".equalsIgnoreCase(String.valueOf(entry.getKey()))) {
                    Integer dimension = parseInteger(entry.getValue());
                    if (dimension != null) {
                        return dimension;
                    }
                }
                Integer nested = extractDimension(entry.getValue());
                if (nested != null) {
                    return nested;
                }
            }
            return null;
        }

        if (source instanceof List<?> list) {
            for (int index = 0; index < list.size(); index++) {
                Object current = list.get(index);
                if ("DIM".equalsIgnoreCase(String.valueOf(current)) && index + 1 < list.size()) {
                    Integer dimension = parseInteger(list.get(index + 1));
                    if (dimension != null) {
                        return dimension;
                    }
                }
                Integer nested = extractDimension(current);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    /**
     * 将 FT.INFO 中的数值字段安全转换为整数。
     *
     * @param value 原始值
     * @return 整数结果；无法解析时返回 {@code null}
     */
    public Integer parseInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String stringValue && StringUtils.hasText(stringValue)) {
            try {
                return Integer.parseInt(stringValue.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
