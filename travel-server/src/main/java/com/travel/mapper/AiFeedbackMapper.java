package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.AiFeedback;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 反馈数据访问接口。
 */
@Mapper
public interface AiFeedbackMapper extends BaseMapper<AiFeedback> {
}
