package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.SpotRegion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 景点地区数据访问接口。
 * <p>
 * 地区树的层级展开由业务支撑层处理，Mapper 维持基础表访问即可。
 */
@Mapper
public interface SpotRegionMapper extends BaseMapper<SpotRegion> {
}

