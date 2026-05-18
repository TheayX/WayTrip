package com.travel.service.support.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 管理端列表排序支持类。
 * <p>
 * 仅允许服务端白名单字段参与排序，避免前端参数直接拼接数据库列名。
 */
public final class AdminSortSupport {

    private AdminSortSupport() {
    }

    public static <T> void applySort(
        LambdaQueryWrapper<T> wrapper,
        String sortBy,
        String sortOrder,
        Map<String, SFunction<T, ?>> sortableFields,
        Runnable defaultOrder
    ) {
        SFunction<T, ?> sortField = StringUtils.hasText(sortBy) ? sortableFields.get(sortBy) : null;
        if (sortField == null) {
            defaultOrder.run();
            return;
        }

        if ("desc".equalsIgnoreCase(sortOrder)) {
            wrapper.orderByDesc(sortField);
        } else {
            wrapper.orderByAsc(sortField);
        }
    }
}
