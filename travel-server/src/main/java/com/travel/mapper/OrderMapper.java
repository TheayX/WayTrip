package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单数据访问接口。
 * <p>
 * 订单查询既服务用户端列表，也服务后台统计与状态流转，因此底层访问统一收在这里。
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
