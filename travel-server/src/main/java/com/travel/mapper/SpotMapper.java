package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.dto.home.response.NearbySpotResponse;
import com.travel.entity.Spot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 景点数据访问接口。
 */
@Mapper
public interface SpotMapper extends BaseMapper<Spot> {

    /**
     * 分页查询景点列表，并补充分类和地区名称。
     */
    IPage<Spot> selectSpotPage(Page<Spot> page,
                               @Param("regionId") Long regionId,
                               @Param("categoryId") Long categoryId,
                               @Param("published") Integer published,
                               @Param("keyword") String keyword,
                               @Param("sortBy") String sortBy);

    /**
     * 面向用户端搜索已发布景点，并补充地区名匹配能力。
     */
    IPage<Spot> selectPublishedSearchPage(Page<Spot> page, @Param("keyword") String keyword);

    /**
     * 根据经纬度查询附近景点列表。
     */
    List<NearbySpotResponse.SpotItem> selectNearbySpots(@Param("latitude") BigDecimal latitude,
                                                        @Param("longitude") BigDecimal longitude,
                                                        @Param("limit") Integer limit);
}
