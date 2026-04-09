package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问接口。
 * <p>
 * 用户表既服务认证，也服务后台用户管理，因此基础访问统一由该 Mapper 提供。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
