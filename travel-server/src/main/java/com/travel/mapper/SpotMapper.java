package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.entity.Spot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SpotMapper extends BaseMapper<Spot> {

    /**
     * 分页查询景点（带分类和地区名称）
     */
    IPage<Spot> selectSpotPage(Page<Spot> page,
                               @Param("regionId") Long regionId,
                               @Param("categoryId") Long categoryId,
                               @Param("published") Integer published,
                               @Param("keyword") String keyword,
                               @Param("sortBy") String sortBy);
}
