package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.dto.spot.SpotDetailResponse;
import com.travel.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    /**
     * 分页查询景点评论（带用户信息）
     */
    IPage<Review> selectRatingPage(Page<Review> page, @Param("spotId") Long spotId);

    /**
     * 查询景点最新评论
     */
    List<SpotDetailResponse.CommentItem> selectLatestComments(@Param("spotId") Long spotId, @Param("limit") int limit);
}

