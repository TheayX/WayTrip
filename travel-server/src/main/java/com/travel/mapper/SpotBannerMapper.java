package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.SpotBanner;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 景点轮播图数据访问接口。
 */
@Mapper
public interface SpotBannerMapper extends BaseMapper<SpotBanner> {

    /**
     * 查询已启用的轮播图列表，并补充关联景点名称。
     */
    List<SpotBanner> selectEnabledBanners();
}

