package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.SpotCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 景点分类数据访问接口。
 */
@Mapper
public interface SpotCategoryMapper extends BaseMapper<SpotCategory> {
}
