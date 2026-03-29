package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.UserPreference;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户偏好标签数据访问接口。
 */
@Mapper
public interface UserPreferenceMapper extends BaseMapper<UserPreference> {
}
