package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.UserSpotView;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户景点浏览记录数据访问接口。
 */
@Mapper
public interface UserSpotViewMapper extends BaseMapper<UserSpotView> {
}
