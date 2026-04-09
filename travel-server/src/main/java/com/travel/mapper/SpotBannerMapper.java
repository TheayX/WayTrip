package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.SpotBanner;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 景点轮播图数据访问接口。
 * <p>
 * 轮播图大多走通用 CRUD，启用列表等面向首页的查询放到定制 SQL 中维护。
 */
@Mapper
public interface SpotBannerMapper extends BaseMapper<SpotBanner> {

    /**
     * 查询已启用的轮播图列表，并补充关联景点名称。
     */
    List<SpotBanner> selectEnabledBanners();
}

