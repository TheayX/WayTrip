package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.SpotImage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 景点图片数据访问接口。
 * <p>
 * 景点图片采用独立表维护顺序与软删除状态，便于后台编辑时整体替换。
 */
@Mapper
public interface SpotImageMapper extends BaseMapper<SpotImage> {
}
