package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.Admin;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员数据访问接口。
 * <p>
 * 管理员表主要走 MyBatis-Plus 通用能力，少量定制查询由服务层通过条件构造器完成。
 */
@Mapper
public interface AdminMapper extends BaseMapper<Admin> {
}
