package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.SpotImage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 景点图片数据访问接口。
 */
@Mapper
public interface SpotImageMapper extends BaseMapper<SpotImage> {
}
