package com.travel.common.result;

import lombok.Data;

import java.util.List;

/**
 * 分页结果对象。
 *
 * @param <T> 列表项类型
 */
@Data
public class PageResult<T> {

    private List<T> list;
    private long total;
    private int page;
    private int pageSize;
    private int totalPages;

    public PageResult() {
    }

    /**
     * 创建分页结果对象。
     *
     * @param list 当前页数据
     * @param total 总记录数
     * @param page 当前页码
     * @param pageSize 每页条数
     */
    public PageResult(List<T> list, long total, int page, int pageSize) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = (int) Math.ceil((double) total / pageSize);
    }

    /**
     * 静态工厂方法，用于快速创建分页结果。
     *
     * @param list 当前页数据
     * @param total 总记录数
     * @param page 当前页码
     * @param pageSize 每页条数
     * @param <T> 列表项类型
     * @return 分页结果对象
     */
    public static <T> PageResult<T> of(List<T> list, long total, int page, int pageSize) {
        return new PageResult<>(list, total, page, pageSize);
    }
}
