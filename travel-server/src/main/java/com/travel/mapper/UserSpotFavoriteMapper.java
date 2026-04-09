package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.UserSpotFavorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户景点收藏数据访问接口。
 * <p>
 * 收藏表使用软删除恢复收藏状态，服务层通过该 Mapper 复用同一条记录。
 */
@Mapper
public interface UserSpotFavoriteMapper extends BaseMapper<UserSpotFavorite> {
}

