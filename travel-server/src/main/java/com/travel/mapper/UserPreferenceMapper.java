package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.UserPreference;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户偏好标签数据访问接口。
 * <p>
 * 偏好标签同时用于账户设置和推荐冷启动，因此底层访问保持简单稳定。
 */
@Mapper
public interface UserPreferenceMapper extends BaseMapper<UserPreference> {
}
