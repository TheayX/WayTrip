package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.SpotRegion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 景点地区数据访问接口。
 */
@Mapper
public interface SpotRegionMapper extends BaseMapper<SpotRegion> {
}

