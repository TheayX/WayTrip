package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.dto.spot.SpotDetailResponse;
import com.travel.entity.Rating;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RatingMapper extends BaseMapper<Rating> {

    /**
     * 分页查询景点评论（带用户信息）
     */
    IPage<Rating> selectRatingPage(Page<Rating> page, @Param("spotId") Long spotId);
    
    /**
     * 查询景点最新评论
     */
    List<SpotDetailResponse.CommentItem> selectLatestComments(@Param("spotId") Long spotId, @Param("limit") int limit);
}
