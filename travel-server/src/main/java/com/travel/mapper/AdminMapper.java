package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.Admin;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员数据访问接口。
 */
@Mapper
public interface AdminMapper extends BaseMapper<Admin> {
}
