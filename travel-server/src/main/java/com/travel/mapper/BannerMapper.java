package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.Banner;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BannerMapper extends BaseMapper<Banner> {

    /**
     * 查询启用的轮播图（带景点名称）
     */
    List<Banner> selectEnabledBanners();
}
