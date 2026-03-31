package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.dto.home.RecentViewedSpotItem;
import com.travel.entity.UserSpotView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户景点浏览记录数据访问接口。
 */
@Mapper
public interface UserSpotViewMapper extends BaseMapper<UserSpotView> {

    /**
     * 查询最近都在看的景点列表。
     *
     * @param startTime 统计起始时间
     * @param limit 返回条数
     * @return 景点列表
     */
    List<RecentViewedSpotItem> selectRecentViewedSpots(@Param("startTime") LocalDateTime startTime,
                                                       @Param("limit") Integer limit);
}
