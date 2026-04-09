package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.SpotCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 景点分类数据访问接口。
 * <p>
 * 分类树的递归展开在 support 层处理，Mapper 只负责基础持久化访问。
 */
@Mapper
public interface SpotCategoryMapper extends BaseMapper<SpotCategory> {
}
