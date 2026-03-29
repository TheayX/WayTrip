package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.UserSpotFavorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户景点收藏数据访问接口。
 */
@Mapper
public interface UserSpotFavoriteMapper extends BaseMapper<UserSpotFavorite> {
}

